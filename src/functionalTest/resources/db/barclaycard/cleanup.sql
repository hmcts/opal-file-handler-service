DELETE FROM public.interface_files
WHERE source = 'BARCLAYCARD'
  AND file_name = 'a121_00010065_317608.dat';

DELETE FROM public.business_unit_bank_account
WHERE business_unit_bank_account_id = 9000000000000021;
