DELETE FROM public.interface_files
WHERE source = 'ALLPAY' AND file_name = 'a121_00350005_300000.dat';

DELETE FROM public.business_unit_bank_account
WHERE business_unit_bank_account_id = 9000000000000023;

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'AB01';
