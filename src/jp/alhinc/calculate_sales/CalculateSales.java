package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	//商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "定義ファイルのフォーマットが不正です";
	private static final String SALE_FILE_INVALID_CODE = "の支店コードが不正です";
	private static final String COMMODITY_FILE_INVALID_CODE = "の商品コードが不正です";
	private static final String SALE_FILE_NOT_SEQUENCE_NUMBER = "売上ファイル名が連番になっていません";
	private static final String RCD_FILE_INVALID_FORMAT = "のフォーマットが不正です";
	private static final String SALE_AMOUNT_OVER_TEN_DIGITS = "合計金額が10桁を超えました";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		//商品コードと商品名を保持するマップ
		Map<String, String> commodityNames = new HashMap<>();
		//商品コードと売上金額を保持するマップ
		Map<String, Long> commoditySales = new HashMap<>();

		// コマンド引数が1つか確認
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales, "^\\d{3}$", "支店")) {
			return;
		}
		//商品定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales,
				"^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8}$", "商品")) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		//支店売上ファイルを抽出
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();
		//ファイルかどうか、ファイル名が正しいか確認
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile() && files[i].getName().matches("^\\d{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}
		//売上ファイルが連番か確認
		Collections.sort(rcdFiles);
		for (int i = 0; i < rcdFiles.size() - 1; i++) {

			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			if (latter - former != 1) {
				System.out.println(SALE_FILE_NOT_SEQUENCE_NUMBER);
				return;
			}
		}

		//支店売上ファイル読込処理
		BufferedReader br = null;
		for (int i = 0; i < rcdFiles.size(); i++) {
			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);

				String line;
				List<String> contents = new ArrayList<>();

				while ((line = br.readLine()) != null) {
					contents.add(line);
				}
				//売上ファイルが3行か確認
				if (contents.size() != 3) {
					System.out.println(rcdFiles.get(i).getName() + RCD_FILE_INVALID_FORMAT);
					return;
				}
				//支店コードが支店定義ファイルに存在しているか確認
				if (!branchNames.containsKey(contents.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + SALE_FILE_INVALID_CODE);
					return;
				}
				//商品コードが商品定義ファイルに存在しているか確認
				if (!commodityNames.containsKey(contents.get(1))) {
					System.out.println(rcdFiles.get(i).getName() + COMMODITY_FILE_INVALID_CODE);
					return;
				}
				//売上ファイルの金額が数字か確認
				if (!contents.get(2).matches("^\\d{1,10}$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}
				//売上を合算しマップに格納
				long fileSale = Long.parseLong(contents.get(2));
				String branchCode = contents.get(0);
				String commodityCode = contents.get(1);
				Long branchSaleAmount = branchSales.get(branchCode) + fileSale;
				Long commoditySaleAmount = commoditySales.get(commodityCode) + fileSale;
				branchSales.put(contents.get(0), branchSaleAmount);
				commoditySales.put(contents.get(1), commoditySaleAmount);

				//売上金額が10桁超えているか
				if (branchSaleAmount >= 10000000000L || commoditySaleAmount >= 10000000000L) {
					System.out.println(SALE_AMOUNT_OVER_TEN_DIGITS);
					return;
				}
			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}
		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}
	}

	/**
	 * 支店/商品定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店/商品コードと支店/商品名を保持するMap
	 * @param 支店/商品コードと支店別/商品別売上金額を保持するMap
	 * @return 読み込み可否
	 */

	private static boolean readFile(String path, String fileName, Map<String, String> names, Map<String, Long> sales,
			String regulation, String category) {
		BufferedReader br = null;
		try {
			File file = new File(path, fileName);
			//ファイルの存在チェック
			if (!file.exists()) {
				System.out.println(category + FILE_NOT_EXIST);
				return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				String[] items = line.split(",");
				//ファイルのフォーマットを確認
				if ((items.length != 2) || (!items[0].matches(regulation))) {
					System.out.println(category + FILE_INVALID_FORMAT);
					return false;
				}
				names.put(items[0], items[1]);
				sales.put(items[0], 0L);
			}
		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店/商品別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店/商品コードと支店名/商品名を保持するMap
	 * @param 支店/商品コードと支店別/商品別売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> names,
			Map<String, Long> sales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		//支店別集計ファイル書込処理
		BufferedWriter bw = null;
		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : sales.keySet()) {
				bw.write(key + "," + names.get(key) + "," + sales.get(key));
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}
}