-- Store the storage-provider (ImageKit) file identifier so the asset can be
-- managed/deleted later. image_url continues to hold the public URL.
ALTER TABLE proof_of_delivery ADD COLUMN IF NOT EXISTS image_file_id VARCHAR(255);
