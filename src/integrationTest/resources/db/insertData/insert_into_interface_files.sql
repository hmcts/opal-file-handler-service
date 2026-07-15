/**
* OPAL Program
*
* MODULE      : insert_into_interface_files.sql
*
* DESCRIPTION : Inserts rows of data into the INTERFACE_FILES table for the Integration Tests.
*
* VERSION HISTORY:
*
* Date        Author   Version  Nature of Change
* ----------  -------  -------  -----------------------------------------------------------------------------------------
* 15/07/2026  J MUSCOTT   1.0   PO-3947: Insert data into interface_files table for intgeration tests.
*
**/


INSERT INTO public.interface_files
(source, target, type, opal_domain, file_name, filestore_uuid, checksum, status, created_datetime, errors)
values
('CAPS_REPORT', 'OPAL', 'SOURCE', 'FINES', 'CAPS-1.xml', '4fb23a4c-c218-4eb6-ac8b-554480ac9805', null, 'INGESTED', CURRENT_TIMESTAMP, null),
('CAPS_REPORT', 'OPAL', 'SOURCE', 'MAINTENANCE', 'CAPS-2.xml', '1b1ef8a3-f722-41de-95b0-fe9cfc3b0922', null, 'SUCCESS', CURRENT_TIMESTAMP, null),
('BTECKOH_REPORT', 'OPAL', 'SOURCE', 'FINES', '2498-Payments-Report-Daily.xlsx', '68b49e2d-74fb-4e26-b068-57efeb33a771', null, 'SUCCESS', CURRENT_TIMESTAMP, null),
('BTECKOH_REPORT', 'OPAL', 'SOURCE', 'FILE_HANDLER', '2500-Payments-Report-Daily.xlsx', 'a5695e1e-bd9f-4a5b-ae15-9deeed2d1384', null, 'FAILED', to_timestamp('2026-01-04 12:30:00', 'YYYY-MM-DD HH24:MI:SS'), '{"error":"malformed xlsx"}');