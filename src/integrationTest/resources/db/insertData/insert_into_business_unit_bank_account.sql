DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'AB01'
  AND bank_sort_code = '560033'
  AND bank_account_number = '27048527';

INSERT INTO public.business_unit_bank_account
    (business_unit_code, opal_domain, bank_sort_code, bank_account_number, dwp_court_code)
VALUES
    ('AB01', 'MAINTENANCE', '560033', '27048527', null);
