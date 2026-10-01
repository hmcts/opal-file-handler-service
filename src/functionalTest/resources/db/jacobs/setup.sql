DELETE FROM public.business_unit_bank_account
WHERE business_unit_bank_account_id = 9000000000000024;

DELETE FROM public.business_unit_bank_account
WHERE bank_sort_code = '560037'
  AND bank_account_number = '27048531';

INSERT INTO public.business_unit_bank_account
    (business_unit_bank_account_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
VALUES (9000000000000024, 'JA01', 'FINES', '560037', '27048531', '0000031716');
