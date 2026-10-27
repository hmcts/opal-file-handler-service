DELETE FROM public.business_unit_bank_account
WHERE business_unit_bank_account_id = 9000000000000022;

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'BT01';

INSERT INTO public.business_unit_bank_account
    (business_unit_bank_account_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
VALUES (9000000000000022, 'BT01', 'MAINTENANCE', '010102', '12341235', NULL);
