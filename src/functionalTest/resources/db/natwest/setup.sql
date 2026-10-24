DELETE FROM public.business_unit_bank_account
WHERE business_unit_id = 3027;

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'NW01';

DELETE FROM public.business_unit_bank_account
WHERE bank_sort_code = '560035'
  AND bank_account_number = '27048529';

INSERT INTO public.business_unit_bank_account
    (business_unit_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
VALUES (3027, 'NW01', 'FINES', '560035', '27048529', '0000031714');
