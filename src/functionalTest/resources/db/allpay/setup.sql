DELETE FROM public.business_unit_bank_account
WHERE business_unit_bank_account_id = 9000000000000023;

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'AB01';

INSERT INTO public.business_unit_bank_account
    (business_unit_bank_account_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
VALUES (9000000000000023, 'AB01', 'MAINTENANCE', '010101', '12341234', NULL);
