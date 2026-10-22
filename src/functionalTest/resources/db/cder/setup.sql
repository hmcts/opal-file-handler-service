DELETE FROM public.business_unit_bank_account
WHERE business_unit_bank_account_id = 9000000000000025;

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'CD12';

DELETE FROM public.business_unit_bank_account
WHERE bank_sort_code = '560036'
  AND bank_account_number = '27048530';

INSERT INTO public.business_unit_bank_account
    (business_unit_bank_account_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
VALUES (9000000000000025, 'CD12', 'FINES', '560036', '27048530', '0000031715');
