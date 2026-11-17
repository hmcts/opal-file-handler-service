/**
* OPAL Program
*
* MODULE      : insert_business_unit_bank_account_for_file_handler.sql
*
* DESCRIPTION : File-Handler - Seed BUSINESS_UNITS_BANK_ACCOUNT table with test data for EI4
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    --------    --------    ----------------------------------------------------------------------------
* 21/09/2026    TT          1.0         PO-8839 - File-Handler - Seed BUSINESS_UNITS_BANK_ACCOUNT table with test data for EI4
*                                       Exclude business units that are not of type 'Accounting Division'.
*                                       London BUs (ID 65, 73, 77 and 78) should share the same bank sort code and account number as BU 65.
*                                       Their stored business_unit_id and business_unit_code values remain unchanged.
*
**/
INSERT INTO public.business_unit_bank_account (business_unit_id, business_unit_code, opal_domain, bank_sort_code, bank_account_number, dwp_court_code)
VALUES
(5, '0009', 'FINES', '500009', '50000009', NULL),
(8, '0011', 'FINES', '800011', '80000011', NULL),
(9, '0012', 'FINES', '900012', '90000012', NULL),
(10, '0013', 'FINES', '100013', '10000013', NULL),
(11, '0020', 'FINES', '110020', '11000020', NULL),
(12, '0021', 'FINES', '120021', '12000021', NULL),
(14, '0051', 'FINES', '140051', '14000051', NULL),
(21, '0082', 'FINES', '210082', '21000082', NULL),
(22, '0084', 'FINES', '220084', '22000084', NULL),
(24, '0121', 'FINES', '240121', '24000121', NULL),
(26, '0069', 'FINES', '260069', '26000069', NULL),
(28, '0095', 'FINES', '280095', '28000095', NULL),
(29, '0096', 'FINES', '290096', '29000096', NULL),
(30, '0093', 'FINES', '300093', '30000093', NULL),
(31, '0127', 'FINES', '310127', '31000127', NULL),
(36, '0113', 'FINES', '360113', '36000113', NULL),
(38, '0118', 'FINES', '380118', '38000118', NULL),
(45, '0055', 'CONFISCATION', '450055', '45000055', NULL),
(47, '0003', 'FINES', '470003', '47000003', NULL),
(52, '0008', 'FINES', '520008', '52000008', NULL),
(57, '0017', 'FINES', '570017', '57000017', NULL),
(60, '0022', 'FINES', '600022', '60000022', NULL),
(61, '0023', 'FINES', '610023', '61000023', NULL),
(65, '0034', 'FINES', '650034', '65000034', NULL),
(66, '0035', 'CONFISCATION', '660035', '66000035', NULL),
(73, '0042', 'FINES', '650034', '65000034', NULL),
(77, '0046', 'FINES', '650034', '65000034', NULL),
(78, '0047', 'FINES', '650034', '65000034', NULL),
(80, '0049', 'FINES', '800049', '80000049', NULL),
(82, '0056', 'FINES', '820056', '82000056', NULL),
(89, '0063', 'FINES', '890063', '89000063', NULL),
(92, '0066', 'FINES', '920066', '92000066', NULL),
(96, '0073', 'FINES', '960073', '96000073', NULL),
(97, '0074', 'CONFISCATION', '970074', '97000074', NULL),
(99, '0076', 'FINES', '990076', '99000076', NULL),
(103, '0080', 'FINES', '103080', '10300080', NULL),
(105, '0090', 'FINES', '105090', '10500090', NULL),
(106, '0094', 'FINES', '106094', '10600094', NULL),
(107, '0098', 'FINES', '107098', '10700098', NULL),
(109, '0100', 'CONFISCATION', '109100', '10900100', NULL),
(110, '0101', 'CONFISCATION', '110101', '11000101', NULL),
(112, '0103', 'FINES', '112103', '11200103', NULL),
(113, '0104', 'FINES', '113104', '11300104', NULL),
(116, '0107', 'CONFISCATION', '116107', '11600107', NULL),
(119, '0110', 'FINES', '119110', '11900110', NULL),
(124, '0117', 'FINES', '124117', '12400117', NULL),
(125, '0122', 'FINES', '125122', '12500122', NULL),
(126, '0123', 'FINES', '126123', '12600123', NULL),
(128, '0128', 'FINES', '128128', '12800128', NULL),
(129, '0129', 'FINES', '129129', '12900129', NULL),
(130, '0130', 'FINES', '130130', '13000130', NULL),
(135, '0135', 'FINES', '135135', '13500135', NULL),
(138, '0138', 'FINES', '138138', '13800138', NULL),
(139, '0139', 'FINES', '139139', '13900139', NULL);
