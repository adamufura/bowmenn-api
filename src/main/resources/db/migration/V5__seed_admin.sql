-- Password is 'Admin@123' (bcrypt hashed)
INSERT INTO users (id, full_name, email, phone, password_hash, role, is_active)
VALUES (
    gen_random_uuid(),
    'Bowmenn Admin',
    'admin@bowmenn.com',
    '+2348000000000',
    '$2b$10$pVvJwpgoG2YYBKklUXsXLOdteeumBi130HSmlQoxiYUal63OF78HG',
    'ADMIN',
    true
) ON CONFLICT (email) DO NOTHING;
