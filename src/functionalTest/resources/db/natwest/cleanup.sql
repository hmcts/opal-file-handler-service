DELETE FROM public.interface_files
WHERE source = 'NATWEST' AND file_name = 'Y01A.CARS.#D.SBURZ38.D080426';

DELETE FROM public.business_unit_bank_account
WHERE business_unit_bank_account_id = 9000000000000027;
