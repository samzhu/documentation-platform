/**
 * Button 組件 - 封裝 Apple Liquid Glass 設計系統按鈕
 *
 * 功能特色：
 * - 支援多種變體（primary, secondary, danger, success, ghost, aurora, warning, info, outline）
 * - 支援三種尺寸（sm, md, lg）
 * - 支援圓形按鈕
 * - 支援載入狀態
 * - 支援圖標（左側或右側）
 * - 支援多態（button, a, Link）
 * - 完整的可及性支援
 */
import React, { forwardRef } from 'react';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';

/**
 * Button 組件
 *
 * @example
 * // 基本用法
 * <Button variant="primary">主要按鈕</Button>
 *
 * @example
 * // 帶圖標
 * <Button variant="success" icon={<CheckIcon />}>確認</Button>
 *
 * @example
 * // 載入狀態
 * <Button variant="primary" loading>處理中...</Button>
 *
 * @example
 * // 作為 Link 使用
 * <Button as={Link} to="/create" variant="aurora">建立文件</Button>
 *
 * @example
 * // 圓形按鈕
 * <Button variant="primary" circle icon={<PlusIcon />} />
 *
 * @example
 * // Outline 變體
 * <Button variant="outline-danger" size="sm">刪除</Button>
 */
const Button = forwardRef(({
  // 按鈕變體
  variant = 'secondary',
  // 按鈕尺寸
  size = 'md',
  // 是否為圓形按鈕
  circle = false,
  // 是否為載入狀態
  loading = false,
  // 是否禁用
  disabled = false,
  // 圖標元素
  icon = null,
  // 圖標位置（left 或 right）
  iconPosition = 'left',
  // 多態組件（button, a, 或任何 React 組件如 Link）
  as: Component = 'button',
  // 額外的 CSS 類別
  className = '',
  // 子元素（按鈕文字）
  children,
  // 其他 props（如 onClick, href, to 等）
  ...props
}, ref) => {
  // 組合 CSS 類別
  const classes = [
    'btn',
    variant && `btn-${variant}`,
    size !== 'md' && `btn-${size}`,
    circle && 'btn-circle',
    loading && 'btn-loading',
    icon && children && 'btn-icon',
    className
  ].filter(Boolean).join(' ');

  // 處理禁用狀態
  const isDisabled = disabled || loading;

  // 處理按鈕內容
  const renderContent = () => {
    // 圓形按鈕只顯示圖標
    if (circle) {
      return icon;
    }

    // 載入狀態不顯示圖標和文字（由 CSS 控制顯示載入動畫）
    if (loading) {
      return children;
    }

    // 同時有圖標和文字
    if (icon && children) {
      return iconPosition === 'left' ? (
        <>
          {icon}
          <span>{children}</span>
        </>
      ) : (
        <>
          <span>{children}</span>
          {icon}
        </>
      );
    }

    // 只有圖標
    if (icon) {
      return icon;
    }

    // 只有文字
    return children;
  };

  // 處理特殊 props
  const componentProps = {
    ref,
    className: classes,
    disabled: isDisabled,
    ...props
  };

  // 如果是 button 元素，設定 type 預設值
  if (Component === 'button' && !props.type) {
    componentProps.type = 'button';
  }

  // 可及性：載入狀態時加上 aria-busy
  if (loading) {
    componentProps['aria-busy'] = true;
  }

  // 可及性：禁用狀態時加上 aria-disabled（針對非 button 元素）
  if (isDisabled && Component !== 'button') {
    componentProps['aria-disabled'] = true;
    // 移除 onClick 避免觸發
    delete componentProps.onClick;
  }

  return (
    <Component {...componentProps}>
      {renderContent()}
    </Component>
  );
});

Button.displayName = 'Button';

Button.propTypes = {
  /** 按鈕變體 */
  variant: PropTypes.oneOf([
    'primary',
    'secondary',
    'danger',
    'success',
    'ghost',
    'aurora',
    'warning',
    'info',
    'outline-primary',
    'outline-danger',
    'outline-success',
    'outline-warning',
    'outline-info'
  ]),
  /** 按鈕尺寸 */
  size: PropTypes.oneOf(['sm', 'md', 'lg']),
  /** 是否為圓形按鈕 */
  circle: PropTypes.bool,
  /** 是否為載入狀態 */
  loading: PropTypes.bool,
  /** 是否禁用 */
  disabled: PropTypes.bool,
  /** 圖標元素 */
  icon: PropTypes.node,
  /** 圖標位置 */
  iconPosition: PropTypes.oneOf(['left', 'right']),
  /** 多態組件類型 */
  as: PropTypes.elementType,
  /** 額外的 CSS 類別 */
  className: PropTypes.string,
  /** 按鈕內容 */
  children: PropTypes.node
};

export default Button;
