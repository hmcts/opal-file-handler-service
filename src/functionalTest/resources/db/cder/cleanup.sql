DELETE FROM public.interface_files
WHERE source = 'CDER' AND file_name = '0000031712_dat_0000098475_20260408_103500.txt';

DELETE FROM public.business_unit_bank_account
WHERE business_unit_bank_account_id = 9000000000000025;
