DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'JA01';

INSERT INTO public.business_unit_bank_account
    (business_unit_code, opal_domain, bank_sort_code, bank_account_number, dwp_court_code)
VALUES
    ('JA01', 'MAINTENANCE', '560033', '27048527', '0000031714');
