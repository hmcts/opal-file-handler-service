DELETE FROM public.interface_files
WHERE source = 'DWP' AND file_name = '0000015232_dat_0000000612_08011008_111355.txt';

DELETE FROM public.business_unit_bank_account
WHERE business_unit_id = 3020;
