DELETE FROM public.business_unit_bank_account
WHERE business_unit_id = 3020;

INSERT INTO public.business_unit_bank_account
    (business_unit_id, business_unit_code, opal_domain, bank_sort_code, bank_account_number, dwp_court_code)
VALUES (3020, 'DW01', 'MAINTENANCE', '010101', '12341234', 'DWP1234567');
