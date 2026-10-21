DELETE FROM public.interface_files
WHERE source = 'ALLPAY' AND file_name = 'a121_00350005_300000.dat';

DELETE FROM public.business_unit_bank_account
WHERE business_unit_id = 3023;

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code = 'AB01';
