DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'AB01'
  AND bank_sort_code = '560033'
  AND bank_account_number = '27048527';

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'AB02'
  AND bank_sort_code = '010101'
  AND bank_account_number = '12341234';
