INSERT INTO
    interface_files
    (interface_file_id, source, target, type, opal_domain,
     file_name, filestore_uuid, status, created_datetime)
VALUES -- valid bteckoh report
        (1, 'BTECKOH_REPORT', 'BTECKOH_REPORT', 'SOURCE',
        'FILE_HANDLER', '', '0f664b85-a5df-4600-9bb3-3b7092ab8718', 'SUCCESS',
        NOW()),
    -- valid caps report
       (2, 'CAPS_REPORT', 'CAPS_REPORT', 'SOURCE',
        'FILE_HANDLER', '', '73c21773-6f49-438d-a760-78f0ffbedf0d', 'SUCCESS',
        NOW()),
    -- invalid report
       (3, 'CAPS_REPORT', 'CAPS_REPORT', 'SOURCE',
        'FILE_HANDLER', '', '73c21773-6f49-438d-a760-78f0ffbedf0d', 'FAILED',
        NOW()),
    -- report with missing blob
       (4, 'BTECKOH_REPORT', 'BTECKOH_REPORT', 'SOURCE',
        'FILE_HANDLER', '', 'b5fed320-1ad1-47f5-8786-91ba31f1604d', 'SUCCESS',
        NOW());