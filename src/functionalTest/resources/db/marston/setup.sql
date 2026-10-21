DELETE FROM public.business_unit_bank_account
WHERE business_unit_id = 3026;

DELETE FROM public.business_unit_bank_account
WHERE bank_sort_code = '560038'
  AND bank_account_number = '27048532';

INSERT INTO public.business_unit_bank_account
    (business_unit_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
VALUES (3026, 'MR01', 'FINES', '560038', '27048532', 'DWP1234568');
