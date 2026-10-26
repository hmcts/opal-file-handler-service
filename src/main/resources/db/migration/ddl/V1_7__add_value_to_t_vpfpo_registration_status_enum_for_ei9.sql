/**
* OPAL Program
*
* MODULE      : add_value_to_t_vpfpo_registration_status_enum_for_ei9.sql
*
* DESCRIPTION : Add values to t_vpfpo_registration_status_enum for EI9
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 19/10/2026    TT             1.0         PO-10683 - Add values to t_vpfpo_registration_status_enum for EI9.
*
**/

ALTER TYPE t_vpfpo_registration_status_enum ADD VALUE IF NOT EXISTS 'PUBLISHED';
ALTER TYPE t_vpfpo_registration_status_enum ADD VALUE IF NOT EXISTS 'PUBLISHING_FAILED';
