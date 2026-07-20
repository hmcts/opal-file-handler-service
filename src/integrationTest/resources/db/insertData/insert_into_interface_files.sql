INSERT INTO public.interface_files
(interface_file_id, source, target, type, opal_domain, file_name, filestore_uuid, checksum, status, created_datetime, errors)
values
(10, 'CAPS_REPORT', 'OPAL', 'SOURCE', 'FINES', 'CAPS-1.xml', '4fb23a4c-c218-4eb6-ac8b-554480ac9805', null, 'INGESTED', CURRENT_TIMESTAMP, null),
(11, 'CAPS_REPORT', 'OPAL', 'SOURCE', 'MAINTENANCE', 'CAPS-2.xml', '1b1ef8a3-f722-41de-95b0-fe9cfc3b0922', null, 'SUCCESS', CURRENT_TIMESTAMP, null),
(12, 'BTECKOH_REPORT', 'OPAL', 'SOURCE', 'FINES', '2498-Payments-Report-Daily.xlsx', '68b49e2d-74fb-4e26-b068-57efeb33a771', null, 'SUCCESS', CURRENT_TIMESTAMP, null),
(13, 'BTECKOH_REPORT', 'OPAL', 'SOURCE', 'FILE_HANDLER', '2500-Payments-Report-Daily.xlsx', 'a5695e1e-bd9f-4a5b-ae15-9deeed2d1384', null, 'FAILED', to_timestamp('2026-01-04 12:30:00', 'YYYY-MM-DD HH24:MI:SS'), '{"error":"malformed xlsx"}');