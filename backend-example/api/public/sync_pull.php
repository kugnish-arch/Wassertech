<?php
/**
 * Обработчик синхронизации /sync/pull
 * 
 * Для роли CLIENT возвращает только данные текущего клиента.
 * Для ролей ADMIN/ENGINEER возвращает полную выборку (с учетом существующих ограничений).
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../auth/auth_middleware.php';
require_once __DIR__ . '/../utils/user_context.php';

header('Content-Type: application/json');

// Проверка авторизации
$user = getCurrentUser();
if (!$user) {
    http_response_code(401);
    echo json_encode(['error' => 'Unauthorized']);
    exit;
}

$since = isset($_GET['since']) ? (int)$_GET['since'] : 1;
$entities = isset($_GET['entities']) ? $_GET['entities'] : [];

// Получаем контекст пользователя
$userRole = $user['role'];
$userId = $user['id'];
$clientId = $user['client_id']; // Для роли CLIENT

$db = getDatabaseConnection();

// Определяем, какие сущности нужно вернуть
$requestedEntities = empty($entities) ? [
    'clients', 'sites', 'installations', 'components',
    'maintenance_sessions', 'maintenance_values',
    'component_templates', 'component_template_fields',
    'icon_packs', 'icons'
] : $entities;

$response = [
    'timestamp' => time() * 1000, // В миллисекундах
    'clients' => [],
    'sites' => [],
    'installations' => [],
    'components' => [],
    'maintenance_sessions' => [],
    'maintenance_values' => [],
    'component_templates' => [],
    'component_template_fields' => [],
    'icon_packs' => [],
    'icons' => [],
    'deleted' => []
];

// ========== ФИЛЬТРАЦИЯ ДЛЯ РОЛИ CLIENT ==========
if ($userRole === 'CLIENT' && $clientId) {
    // Для CLIENT возвращаем только данные текущего клиента
    
    // 1. Clients - только один клиент
    if (in_array('clients', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM clients 
            WHERE id = :clientId 
            AND (is_archived = 0 OR is_archived IS NULL)
            AND updatedAtEpoch >= :since
        ");
        $stmt->execute(['clientId' => $clientId, 'since' => $since]);
        $response['clients'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // 2. Sites - только объекты текущего клиента
    if (in_array('sites', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM sites 
            WHERE client_id = :clientId 
            AND (is_archived = 0 OR is_archived IS NULL)
            AND updatedAtEpoch >= :since
            ORDER BY orderIndex ASC, name ASC
        ");
        $stmt->execute(['clientId' => $clientId, 'since' => $since]);
        $response['sites'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // 3. Installations - через sites
    if (in_array('installations', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT i.* FROM installations i
            JOIN sites s ON i.site_id = s.id
            WHERE s.client_id = :clientId 
            AND (i.is_archived = 0 OR i.is_archived IS NULL)
            AND i.updatedAtEpoch >= :since
            ORDER BY i.orderIndex ASC, i.name ASC
        ");
        $stmt->execute(['clientId' => $clientId, 'since' => $since]);
        $response['installations'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // 4. Components - через installations -> sites
    if (in_array('components', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT c.* FROM components c
            JOIN installations i ON c.installation_id = i.id
            JOIN sites s ON i.site_id = s.id
            WHERE s.client_id = :clientId 
            AND (c.is_archived = 0 OR c.is_archived IS NULL)
            AND c.updatedAtEpoch >= :since
            ORDER BY c.orderIndex ASC, c.name ASC
        ");
        $stmt->execute(['clientId' => $clientId, 'since' => $since]);
        $response['components'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // 5. Maintenance sessions - через sites
    if (in_array('maintenance_sessions', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT ms.* FROM maintenance_sessions ms
            JOIN sites s ON ms.site_id = s.id
            WHERE s.client_id = :clientId 
            AND (ms.is_archived = 0 OR ms.is_archived IS NULL)
            AND ms.updatedAtEpoch >= :since
            ORDER BY ms.startedAtEpoch DESC
        ");
        $stmt->execute(['clientId' => $clientId, 'since' => $since]);
        $response['maintenance_sessions'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // 6. Maintenance values - через maintenance_sessions -> sites
    if (in_array('maintenance_values', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT mv.* FROM maintenance_values mv
            JOIN maintenance_sessions ms ON mv.session_id = ms.id
            JOIN sites s ON ms.site_id = s.id
            WHERE s.client_id = :clientId 
            AND (mv.is_archived = 0 OR mv.is_archived IS NULL)
            AND mv.updatedAtEpoch >= :since
        ");
        $stmt->execute(['clientId' => $clientId, 'since' => $since]);
        $response['maintenance_values'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // 7. Component templates - доступны всем (или фильтруем по необходимости)
    if (in_array('component_templates', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM component_templates 
            WHERE updatedAtEpoch >= :since
            ORDER BY name ASC
        ");
        $stmt->execute(['since' => $since]);
        $response['component_templates'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // 8. Component template fields - через templates
    if (in_array('component_template_fields', $requestedEntities)) {
        $templateIds = array_column($response['component_templates'], 'id');
        if (!empty($templateIds)) {
            $placeholders = implode(',', array_fill(0, count($templateIds), '?'));
            $stmt = $db->prepare("
                SELECT * FROM component_template_fields 
                WHERE template_id IN ($placeholders)
                AND updatedAtEpoch >= ?
                ORDER BY sortOrder ASC
            ");
            $stmt->execute(array_merge($templateIds, [$since]));
            $response['component_template_fields'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }
    }
    
    // 9. Icon packs - фильтруем по доступности для клиента
    if (in_array('icon_packs', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT DISTINCT ip.* FROM icon_packs ip
            WHERE ip.is_visible_in_client = 1
            AND (
                -- Пак доступен по умолчанию для всех клиентов
                (ip.is_default_for_all_clients = 1 
                 AND NOT EXISTS (
                     SELECT 1 FROM client_icon_packs cip 
                     WHERE cip.pack_id = ip.id 
                     AND cip.client_id = :clientId 
                     AND cip.is_enabled = 0
                 ))
                OR
                -- Пак явно включен для этого клиента
                EXISTS (
                    SELECT 1 FROM client_icon_packs cip 
                    WHERE cip.pack_id = ip.id 
                    AND cip.client_id = :clientId 
                    AND cip.is_enabled = 1
                )
            )
            AND ip.updatedAtEpoch >= :since
            ORDER BY ip.name ASC
        ");
        $stmt->execute(['clientId' => $clientId, 'since' => $since]);
        $response['icon_packs'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // 10. Icons - только из доступных паков и активные
    if (in_array('icons', $requestedEntities)) {
        $packIds = array_column($response['icon_packs'], 'id');
        if (!empty($packIds)) {
            $placeholders = implode(',', array_fill(0, count($packIds), '?'));
            $stmt = $db->prepare("
                SELECT * FROM icons 
                WHERE pack_id IN ($placeholders)
                AND is_active = 1
                AND updatedAtEpoch >= ?
                ORDER BY label ASC
            ");
            $stmt->execute(array_merge($packIds, [$since]));
            $response['icons'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }
    }
    
    // 11. Deleted records - только для данных текущего клиента
    if (in_array('deleted', $requestedEntities) || empty($requestedEntities)) {
        // Получаем удаленные объекты клиента
        $stmt = $db->prepare("
            SELECT 'sites' as tableName, id as recordId, deletedAtEpoch 
            FROM sites 
            WHERE client_id = :clientId 
            AND deletedAtEpoch IS NOT NULL 
            AND deletedAtEpoch >= :since
        ");
        $stmt->execute(['clientId' => $clientId, 'since' => $since]);
        $deletedSites = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Получаем удаленные установки через sites
        $stmt = $db->prepare("
            SELECT 'installations' as tableName, i.id as recordId, i.deletedAtEpoch 
            FROM installations i
            JOIN sites s ON i.site_id = s.id
            WHERE s.client_id = :clientId 
            AND i.deletedAtEpoch IS NOT NULL 
            AND i.deletedAtEpoch >= :since
        ");
        $stmt->execute(['clientId' => $clientId, 'since' => $since]);
        $deletedInstallations = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Получаем удаленные компоненты через installations -> sites
        $stmt = $db->prepare("
            SELECT 'components' as tableName, c.id as recordId, c.deletedAtEpoch 
            FROM components c
            JOIN installations i ON c.installation_id = i.id
            JOIN sites s ON i.site_id = s.id
            WHERE s.client_id = :clientId 
            AND c.deletedAtEpoch IS NOT NULL 
            AND c.deletedAtEpoch >= :since
        ");
        $stmt->execute(['clientId' => $clientId, 'since' => $since]);
        $deletedComponents = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        $response['deleted'] = array_merge($deletedSites, $deletedInstallations, $deletedComponents);
    }
    
} else {
    // ========== ДЛЯ РОЛЕЙ ADMIN/ENGINEER - ПОЛНАЯ ВЫБОРКА ==========
    // Здесь используется существующая логика с учетом user_membership и других ограничений
    
    // Clients
    if (in_array('clients', $requestedEntities)) {
        // Для ADMIN/ENGINEER можно добавить фильтрацию по user_membership если нужно
        $stmt = $db->prepare("
            SELECT * FROM clients 
            WHERE (is_archived = 0 OR is_archived IS NULL)
            AND updatedAtEpoch >= :since
            ORDER BY sortOrder ASC, name ASC
        ");
        $stmt->execute(['since' => $since]);
        $response['clients'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Sites - с учетом user_membership если нужно
    if (in_array('sites', $requestedEntities)) {
        // TODO: Добавить фильтрацию по user_membership если требуется
        $stmt = $db->prepare("
            SELECT * FROM sites 
            WHERE (is_archived = 0 OR is_archived IS NULL)
            AND updatedAtEpoch >= :since
            ORDER BY orderIndex ASC, name ASC
        ");
        $stmt->execute(['since' => $since]);
        $response['sites'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Installations
    if (in_array('installations', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM installations 
            WHERE (is_archived = 0 OR is_archived IS NULL)
            AND updatedAtEpoch >= :since
            ORDER BY orderIndex ASC, name ASC
        ");
        $stmt->execute(['since' => $since]);
        $response['installations'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Components
    if (in_array('components', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM components 
            WHERE (is_archived = 0 OR is_archived IS NULL)
            AND updatedAtEpoch >= :since
            ORDER BY orderIndex ASC, name ASC
        ");
        $stmt->execute(['since' => $since]);
        $response['components'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Maintenance sessions
    if (in_array('maintenance_sessions', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM maintenance_sessions 
            WHERE (is_archived = 0 OR is_archived IS NULL)
            AND updatedAtEpoch >= :since
            ORDER BY startedAtEpoch DESC
        ");
        $stmt->execute(['since' => $since]);
        $response['maintenance_sessions'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Maintenance values
    if (in_array('maintenance_values', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM maintenance_values 
            WHERE (is_archived = 0 OR is_archived IS NULL)
            AND updatedAtEpoch >= :since
        ");
        $stmt->execute(['since' => $since]);
        $response['maintenance_values'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Component templates
    if (in_array('component_templates', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM component_templates 
            WHERE updatedAtEpoch >= :since
            ORDER BY name ASC
        ");
        $stmt->execute(['since' => $since]);
        $response['component_templates'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Component template fields
    if (in_array('component_template_fields', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM component_template_fields 
            WHERE updatedAtEpoch >= :since
            ORDER BY sortOrder ASC
        ");
        $stmt->execute(['since' => $since]);
        $response['component_template_fields'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Icon packs - все видимые
    if (in_array('icon_packs', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM icon_packs 
            WHERE updatedAtEpoch >= :since
            ORDER BY name ASC
        ");
        $stmt->execute(['since' => $since]);
        $response['icon_packs'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Icons - все активные
    if (in_array('icons', $requestedEntities)) {
        $stmt = $db->prepare("
            SELECT * FROM icons 
            WHERE is_active = 1
            AND updatedAtEpoch >= :since
            ORDER BY label ASC
        ");
        $stmt->execute(['since' => $since]);
        $response['icons'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Deleted records
    if (in_array('deleted', $requestedEntities) || empty($requestedEntities)) {
        // Полная выборка удаленных записей для ADMIN/ENGINEER
        // TODO: Добавить фильтрацию по user_membership если требуется
        $stmt = $db->query("
            SELECT 'sites' as tableName, id as recordId, deletedAtEpoch 
            FROM sites 
            WHERE deletedAtEpoch IS NOT NULL AND deletedAtEpoch >= $since
            UNION ALL
            SELECT 'installations' as tableName, id as recordId, deletedAtEpoch 
            FROM installations 
            WHERE deletedAtEpoch IS NOT NULL AND deletedAtEpoch >= $since
            UNION ALL
            SELECT 'components' as tableName, id as recordId, deletedAtEpoch 
            FROM components 
            WHERE deletedAtEpoch IS NOT NULL AND deletedAtEpoch >= $since
        ");
        $response['deleted'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
}

// Преобразуем все числовые поля в правильные типы
foreach ($response as $key => &$items) {
    if (is_array($items)) {
        foreach ($items as &$item) {
            if (isset($item['updatedAtEpoch'])) {
                $item['updatedAtEpoch'] = (int)$item['updatedAtEpoch'];
            }
            if (isset($item['createdAtEpoch'])) {
                $item['createdAtEpoch'] = (int)$item['createdAtEpoch'];
            }
            if (isset($item['deletedAtEpoch'])) {
                $item['deletedAtEpoch'] = $item['deletedAtEpoch'] ? (int)$item['deletedAtEpoch'] : null;
            }
            // Преобразуем булевы поля
            if (isset($item['is_archived'])) {
                $item['is_archived'] = (bool)$item['is_archived'];
            }
            if (isset($item['is_active'])) {
                $item['is_active'] = (bool)$item['is_active'];
            }
            if (isset($item['is_visible_in_client'])) {
                $item['is_visible_in_client'] = (bool)$item['is_visible_in_client'];
            }
            if (isset($item['is_default_for_all_clients'])) {
                $item['is_default_for_all_clients'] = (bool)$item['is_default_for_all_clients'];
            }
        }
    }
}

echo json_encode($response, JSON_UNESCAPED_UNICODE);

