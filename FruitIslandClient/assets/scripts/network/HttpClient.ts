import { sys } from 'cc';
import { ServerResult } from '../types';

/**
 * 统一 HTTP 请求客户端
 *
 * 功能：
 * - 自动拼接 baseUrl
 * - 自动携带 Authorization Token
 * - 统一错误处理
 * - 支持 GET / POST
 */
export class HttpClient {
  private static instance: HttpClient;

  /** 服务器基础地址 */
  private baseUrl: string = 'http://localhost:8080';

  /** 登录后保存的 Token */
  private token: string = '';

  /** 请求超时时间（毫秒） */
  private timeout: number = 10000;

  static getInstance(): HttpClient {
    if (!HttpClient.instance) {
      HttpClient.instance = new HttpClient();
    }
    return HttpClient.instance;
  }

  /** 设置服务器地址 */
  setBaseUrl(url: string): void {
    this.baseUrl = url;
  }

  /** 保存登录 Token */
  setToken(token: string): void {
    this.token = token;
    // 同时持久化到本地存储
    sys.localStorage.setItem('auth_token', token);
  }

  /** 获取已保存的 Token */
  getToken(): string {
    if (!this.token) {
      this.token = sys.localStorage.getItem('auth_token') || '';
    }
    return this.token;
  }

  /** 清除 Token */
  clearToken(): void {
    this.token = '';
    sys.localStorage.removeItem('auth_token');
  }

  /** 是否已登录 */
  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  /**
   * GET 请求
   */
  async get<T = any>(url: string, params?: Record<string, any>): Promise<ServerResult<T>> {
    let fullUrl = `${this.baseUrl}${url}`;
    if (params) {
      const query = Object.entries(params)
        .map(([k, v]: [string, any]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
        .join('&');
      fullUrl += `?${query}`;
    }
    return this.request<T>('GET', fullUrl);
  }

  /**
   * POST 请求
   */
  async post<T = any>(url: string, data?: any): Promise<ServerResult<T>> {
    const fullUrl = `${this.baseUrl}${url}`;
    return this.request<T>('POST', fullUrl, data);
  }

  /**
   * 底层请求方法
   */
  private async request<T>(
    method: string,
    url: string,
    body?: any,
  ): Promise<ServerResult<T>> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    // 自动携带 Token
    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    try {
      const options: RequestInit = {
        method,
        headers,
        signal: controller.signal,
      };

      if (body && method === 'POST') {
        options.body = JSON.stringify(body);
      }

      const response = await fetch(url, options);
      clearTimeout(timeoutId);

      const result: ServerResult<T> = await response.json();

      // Token 过期，跳转登录
      if (result.code === 10002) {
        this.clearToken();
        console.warn('[HttpClient] Token expired, please login again');
        // 通知 GameManager 处理
        if (typeof window !== 'undefined') {
          (window as any).__onTokenExpired?.();
        }
      }

      return result;
    } catch (err: any) {
      clearTimeout(timeoutId);

      if (err.name === 'AbortError') {
        return {
          code: -1,
          message: '请求超时',
          data: null as any,
        };
      }

      console.error(`[HttpClient] ${method} ${url} failed:`, err.message);
      return {
        code: -1,
        message: `网络错误: ${err.message}`,
        data: null as any,
      };
    }
  }
}

/** 快捷导出单例 */
export const http = HttpClient.getInstance();
