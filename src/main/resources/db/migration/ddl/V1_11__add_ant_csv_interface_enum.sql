/**
* OPAL Program
*
* MODULE      : add_ant_csv_interface_enum.sql
*
* DESCRIPTION : Add ANT_CSV as an interface target value
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 13/11/2026    TT             1.0         PO-11447 - Add ANT_CSV to t_interface_enum for EI8
*
**/

ALTER TYPE t_interface_enum ADD VALUE IF NOT EXISTS 'ANT_CSV';
