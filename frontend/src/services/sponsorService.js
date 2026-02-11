/**
 * 贊助服務 — x402 付款協議整合
 * <p>
 * 使用 x402 開放協議，透過瀏覽器錢包（MetaMask）進行 USDC 贊助。
 * 流程：前端發送請求 → 後端回 402 → x402 client 自動簽名重試 → 完成付款。
 * </p>
 */

import { createWalletClient, custom } from 'viem';
import { base } from 'viem/chains';
import { wrapFetchWithPayment, x402Client } from '@x402/fetch';
import { registerExactEvmScheme } from '@x402/evm/exact/client';

/**
 * 檢查瀏覽器是否安裝了錢包（MetaMask 等）
 *
 * @returns {boolean} 是否有 window.ethereum 注入
 */
export function isWalletAvailable() {
  return typeof window !== 'undefined' && !!window.ethereum;
}

/**
 * 執行 x402 贊助付款
 * <p>
 * 完整流程：
 * 1. 建立 viem WalletClient（連接 MetaMask）
 * 2. 請求帳號授權 + 切換到 Base Sepolia 鏈
 * 3. 建立 x402 client 並註冊 EVM 簽名方案
 * 4. 透過 wrapFetchWithPayment 自動處理 402 → 簽名 → 重試
 * </p>
 *
 * @param {number} amount - 贊助金額（1、5 或 10 美元）
 * @returns {Promise<Object>} 贊助結果 { success, message, transaction? }
 * @throws {Error} 錢包未安裝、使用者拒絕、付款失敗等
 */
export async function sponsor(amount) {
  if (!isWalletAvailable()) {
    throw new Error('請安裝 MetaMask 錢包');
  }

  // 建立 viem WalletClient，使用 MetaMask 注入的 provider
  const walletClient = createWalletClient({
    chain: base,
    transport: custom(window.ethereum),
  });

  // 請求帳號連接（MetaMask 會彈出授權視窗）
  const [address] = await walletClient.requestAddresses();

  // 嘗試切換到 Base Sepolia 測試網
  try {
    await walletClient.switchChain({ id: base.id });
  } catch (e) {
    // 鏈不存在時，自動添加
    if (e.code === 4902) {
      await walletClient.addChain({ chain: base });
    } else {
      throw e;
    }
  }

  // 建立 ClientEvmSigner 適配器
  // 橋接 viem WalletClient 與 x402 ClientEvmSigner 介面
  const signer = {
    address,
    signTypedData: ({ domain, types, primaryType, message }) =>
      walletClient.signTypedData({
        account: address,
        domain,
        types,
        primaryType,
        message,
      }),
  };

  // 建立 x402 client 並註冊 EVM exact 付款方案
  // registerExactEvmScheme 會同時註冊 V1 和 V2 協議
  const client = new x402Client();
  registerExactEvmScheme(client, { signer });

  // 包裝 fetch，自動處理 402 PaymentRequired 回應
  const fetchWithPayment = wrapFetchWithPayment(fetch, client);

  // 發送贊助請求
  const response = await fetchWithPayment(`/api/sponsor?amount=${amount}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });

  // 解析回應
  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.error || '贊助失敗');
  }

  return data;
}
