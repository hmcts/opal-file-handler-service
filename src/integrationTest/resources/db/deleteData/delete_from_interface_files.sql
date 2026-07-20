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
where interface_file_id in
(10,11,12,13);