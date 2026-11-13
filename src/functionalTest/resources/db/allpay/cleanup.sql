DELETE FROM public.interface_files
WHERE source = 'ALLPAY' AND file_name = 'a121_00350005_300000.dat';

DELETE FROM public.business_unit_bank_account
WHERE business_unit_id IN (3023, 3028, 3029, 3030);

DELETE FROM public.business_unit_bank_account
WHERE business_unit_code IN ('AB01', 'AB02', 'AB03', 'AB04');
