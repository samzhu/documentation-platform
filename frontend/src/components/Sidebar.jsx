/**
 * 側邊欄組件
 * 顯示導航選單和 Logo
 */
import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  DashboardIcon,
  LibraryIcon,
  SearchIcon,
  SyncIcon,
  KeyIcon,
  DocumentIcon,
  SettingsIcon,
  PlayIcon
} from './Icons';

// 導航項目配置（分組）
const navGroups = [
  {
    label: 'Main',
    items: [
      { path: '/', label: 'Dashboard', icon: DashboardIcon },
      { path: '/libraries', label: 'Libraries', icon: LibraryIcon },
      { path: '/search', label: 'Search', icon: SearchIcon },
      { path: '/sync-history', label: 'Sync Status', icon: SyncIcon },
    ]
  },
  {
    label: 'Settings',
    items: [
      { path: '/api-keys', label: 'API Keys', icon: KeyIcon },
      { path: '/settings', label: 'Settings', icon: SettingsIcon },
      { path: '/setup', label: 'Setup', icon: PlayIcon },
    ]
  }
];

export default function Sidebar() {
  const location = useLocation();

  return (
    <aside className="sidebar">
      <div className="sidebar-glass-container">
        <div className="sidebar-content">
          {/* Logo 區塊 */}
          <div className="sidebar-brand">
            <div className="sidebar-logo">
              <span className="sidebar-logo-icon">📚</span>
              <h1>DocHub</h1>
            </div>
          </div>

          {/* 導航選單 - 分組 */}
          <nav className="sidebar-nav">
            {navGroups.map((group, groupIndex) => (
              <div key={groupIndex} className="nav-group">
                <div className="nav-group-label">{group.label}</div>
                {group.items.map(item => {
                  const IconComponent = item.icon;
                  const isActive = location.pathname === item.path;

                  return (
                    <Link
                      key={item.path}
                      to={item.path}
                      className={`nav-item ${isActive ? 'active' : ''}`}
                    >
                      <span className="nav-icon">
                        <IconComponent size={20} />
                      </span>
                      <span className="nav-label">{item.label}</span>
                    </Link>
                  );
                })}
              </div>
            ))}
          </nav>

          {/* 底部資訊 */}
          <div className="sidebar-footer">
            <p>Documentation Platform v1.0.0</p>
          </div>
        </div>
      </div>
    </aside>
  );
}
