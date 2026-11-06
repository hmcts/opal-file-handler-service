DELETE FROM public.interface_files;
INSERT INTO
    public.interface_files
    (interface_file_id, source, target, type, opal_domain,
     file_name, filestore_uuid, checksum, status,
     created_datetime, errors,
     created_by)
VALUES
    -- valid bteckoh report
    (100, 'BTECKOH_REPORT', 'BTECKOH_REPORT', 'SOURCE',
     'FILE_HANDLER', '', '0f664b85-a5df-4600-9bb3-3b7092ab8718', null,
     'SUCCESS', '2023-06-01T10:00:00', null, -1);

DELETE FROM public.vpfpo_registrations;
INSERT INTO
    public.vpfpo_registrations
    (vpfpo_registration_id, interface_file_id, registration_status)
VALUES
    (1000, 100, 'CREATED');