DELETE FROM public.business_unit_bank_account
WHERE business_unit_bank_account_id = 9000000000000021;

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'BC12';

DELETE FROM public.business_unit_bank_account
WHERE bank_sort_code = '560034'
  AND bank_account_number = '27048528';

INSERT INTO public.business_unit_bank_account
    (business_unit_bank_account_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
VALUES
    (9000000000000021, 'BC12', 'FINES', '560034', '27048528', '0000031714');
