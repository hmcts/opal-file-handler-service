/**
* OPAL Program
*
* MODULE      : delete_from_interface_files.sql
*
* DESCRIPTION : Deletes the data from the INTERFACE_FILES table that was inserted for integration tests
*
* VERSION HISTORY:
*
* Date        Author   Version  Nature of Change
* ----------  -------  -------  -----------------------------------------------------------------------------------------
* 15/07/2026  J MUSCOTT   1.0   PO-3947: Delete data from interface_files table that was inserted for intgeration tests.
*
**/

delete from public.interface_files
where filestore_uuid in
('4fb23a4c-c218-4eb6-ac8b-554480ac9805',
'1b1ef8a3-f722-41de-95b0-fe9cfc3b0922',
'68b49e2d-74fb-4e26-b068-57efeb33a771',
'a5695e1e-bd9f-4a5b-ae15-9deeed2d1384');