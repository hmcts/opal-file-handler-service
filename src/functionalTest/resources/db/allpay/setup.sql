DELETE FROM public.business_unit_bank_account
WHERE business_unit_id IN (3023, 3028, 3029, 3030);

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code IN ('AB01', 'AB02', 'AB03', 'AB04');

INSERT INTO public.business_unit_bank_account
    (business_unit_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
SELECT 3023, 'AB01', 'FINES', '803456', '80345678', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM public.business_unit_bank_account
    WHERE bank_sort_code = '803456'
      AND bank_account_number = '80345678'
);

INSERT INTO public.business_unit_bank_account
    (business_unit_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
SELECT 3028, 'AB02', 'FINES', '804567', '80456789', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM public.business_unit_bank_account
    WHERE bank_sort_code = '804567'
      AND bank_account_number = '80456789'
);

INSERT INTO public.business_unit_bank_account
    (business_unit_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
SELECT 3029, 'AB03', 'FINES', '806789', '80678901', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM public.business_unit_bank_account
    WHERE bank_sort_code = '806789'
      AND bank_account_number = '80678901'
);

INSERT INTO public.business_unit_bank_account
    (business_unit_id, business_unit_code, opal_domain,
     bank_sort_code, bank_account_number, dwp_court_code)
SELECT 3030, 'AB04', 'FINES', '678901', '67890123', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM public.business_unit_bank_account
    WHERE bank_sort_code = '678901'
      AND bank_account_number = '67890123'
);
