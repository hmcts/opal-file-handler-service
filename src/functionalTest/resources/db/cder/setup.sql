DELETE FROM public.business_unit_bank_account
WHERE business_unit_id = 3025;

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'CD12';

DELETE FROM public.business_unit_bank_account
WHERE bank_sort_code = '800036'
  AND bank_account_number = '80048530';

INSERT INTO public.business_unit_bank_account
    (business_unit_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
VALUES (3025, 'CD12', 'FINES', '800036', '80048530', '0000083025');
