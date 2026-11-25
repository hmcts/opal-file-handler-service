DELETE FROM public.business_unit_bank_account
WHERE business_unit_id = 3024;

DELETE FROM public.business_unit_bank_account
WHERE bank_sort_code = '800037'
  AND bank_account_number = '80048531';

INSERT INTO public.business_unit_bank_account
    (business_unit_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
VALUES (3024, 'JA01', 'FINES', '800037', '80048531', '0000083024');
