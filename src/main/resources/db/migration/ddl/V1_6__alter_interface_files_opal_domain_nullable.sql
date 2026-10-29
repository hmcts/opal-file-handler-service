/**
* OPAL Program
*
* MODULE      : alter_interface_files_opal_domain_nullable.sql
*
* DESCRIPTION : Alter column interface_files.opal_domain to be nullable.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 15/10/2026    T McCallion    1.0         PO-8686 - BE - Human authentication support / verification
*                                          This change is covered by EI7 but needs bring forward.
*
**/

ALTER TABLE interface_files
    ALTER COLUMN opal_domain DROP NOT NULL;