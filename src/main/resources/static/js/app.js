/**
 * Modern Crypto Community - Main JavaScript
 * Handles common interactions, notifications, and UI enhancements
 */

(function() {
  'use strict';

  // ==========================================================================
  // Utility Functions
  // ==========================================================================

  const utils = {
    // Debounce function for search and other frequent operations
    debounce(func, wait) {
      let timeout;
      return function executedFunction(...args) {
        const later = () => {
          clearTimeout(timeout);
          func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
      };
    },

    // Format numbers with commas
    formatNumber(num) {
      return new Intl.NumberFormat('ko-KR').format(num);
    },

    // Format currency
    formatCurrency(amount, currency = 'USD') {
      return new Intl.NumberFormat('ko-KR', {
        style: 'currency',
        currency: currency,
        minimumFractionDigits: 2
      }).format(amount);
    },

    // Format percentage
    formatPercentage(value) {
      return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;
    },

    // Show toast notification
    showToast(message, type = 'info') {
      const toast = document.createElement('div');
      toast.className = `toast toast--${type}`;
      toast.textContent = message;
      
      document.body.appendChild(toast);
      
      // Trigger animation
      requestAnimationFrame(() => {
        toast.classList.add('toast--show');
      });
      
      // Auto remove after 3 seconds
      setTimeout(() => {
        toast.classList.remove('toast--show');
        setTimeout(() => {
          if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
          }
        }, 300);
      }, 3000);
    },

    // Copy text to clipboard
    async copyToClipboard(text) {
      try {
        await navigator.clipboard.writeText(text);
        this.showToast('클립보드에 복사되었습니다', 'success');
      } catch (err) {
        console.error('Failed to copy text: ', err);
        this.showToast('복사에 실패했습니다', 'danger');
      }
    }
  };

  // ==========================================================================
  // Dropdown Management
  // ==========================================================================

  function initDropdowns() {
    // Close dropdowns when clicking outside
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.dropdown')) {
        document.querySelectorAll('.dropdown--open').forEach(dropdown => {
          dropdown.classList.remove('dropdown--open');
        });
      }
    });

    // Close dropdowns on escape key
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        document.querySelectorAll('.dropdown--open').forEach(dropdown => {
          dropdown.classList.remove('dropdown--open');
        });
      }
    });
  }

  // Global dropdown toggle function (called from HTML)
  window.toggleDropdown = function(dropdownId) {
    const dropdown = document.getElementById(dropdownId);
    if (dropdown) {
      dropdown.classList.toggle('dropdown--open');
    }
  };

  // ==========================================================================
  // Search Enhancement
  // ==========================================================================

  function initSearch() {
    const searchInput = document.querySelector('.search-input');
    if (!searchInput) return;

    // Search suggestions container
    const suggestionsContainer = document.createElement('div');
    suggestionsContainer.className = 'search-suggestions';
    searchInput.parentNode.appendChild(suggestionsContainer);

    // Debounced search function
    const debouncedSearch = utils.debounce(async (query) => {
      if (query.length < 2) {
        suggestionsContainer.style.display = 'none';
        return;
      }

      try {
        const response = await fetch(`/api/search/suggestions?q=${encodeURIComponent(query)}`);
        const suggestions = await response.json();
        
        if (suggestions.length > 0) {
          displaySuggestions(suggestions);
        } else {
          suggestionsContainer.style.display = 'none';
        }
      } catch (error) {
        console.error('Search suggestions error:', error);
      }
    }, 300);

    // Handle input
    searchInput.addEventListener('input', (e) => {
      const query = e.target.value.trim();
      debouncedSearch(query);
    });

    // Hide suggestions when clicking outside
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.search-input-group')) {
        suggestionsContainer.style.display = 'none';
      }
    });

    function displaySuggestions(suggestions) {
      suggestionsContainer.innerHTML = suggestions.map(item => `
        <div class="suggestion-item" data-type="${item.type}" data-value="${item.value}">
          <div class="suggestion-icon">${getIconForType(item.type)}</div>
          <div class="suggestion-content">
            <div class="suggestion-title">${item.title}</div>
            <div class="suggestion-subtitle">${item.subtitle || ''}</div>
          </div>
        </div>
      `).join('');

      suggestionsContainer.style.display = 'block';

      // Handle suggestion clicks
      suggestionsContainer.addEventListener('click', (e) => {
        const suggestion = e.target.closest('.suggestion-item');
        if (suggestion) {
          const type = suggestion.dataset.type;
          const value = suggestion.dataset.value;
          handleSuggestionClick(type, value);
        }
      });
    }

    function getIconForType(type) {
      const icons = {
        coin: '🪙',
        post: '📝',
        user: '👤',
        board: '📋'
      };
      return icons[type] || '🔍';
    }

    function handleSuggestionClick(type, value) {
      const urls = {
        coin: `/coins/${value}`,
        post: `/posts/${value}`,
        user: `/profile/${value}`,
        board: `/boards/${value}`
      };
      
      if (urls[type]) {
        window.location.href = urls[type];
      }
    }
  }

  // ==========================================================================
  // Notification System
  // ==========================================================================

  function initNotifications() {
    const notificationBtn = document.querySelector('.notification-btn');
    if (!notificationBtn) return;

    // Create notification dropdown
    const notificationDropdown = document.createElement('div');
    notificationDropdown.className = 'notification-dropdown dropdown';
    notificationDropdown.innerHTML = `
      <div class="notification-header">
        <h3>알림</h3>
        <button class="btn btn--ghost btn--sm" onclick="markAllNotificationsAsRead()">
          모두 읽음
        </button>
      </div>
      <div class="notification-list">
        <div class="notification-loading">알림을 불러오는 중...</div>
      </div>
      <div class="notification-footer">
        <a href="/notifications" class="btn btn--secondary btn--sm">모든 알림 보기</a>
      </div>
    `;

    notificationBtn.parentNode.appendChild(notificationDropdown);

    // Toggle notification dropdown
    notificationBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      notificationDropdown.classList.toggle('dropdown--open');
      
      if (notificationDropdown.classList.contains('dropdown--open')) {
        loadNotifications();
      }
    });

    // Load notifications
    async function loadNotifications() {
      try {
        const response = await fetch('/api/notifications');
        const notifications = await response.json();
        displayNotifications(notifications);
      } catch (error) {
        console.error('Failed to load notifications:', error);
        notificationDropdown.querySelector('.notification-list').innerHTML = 
          '<div class="notification-error">알림을 불러올 수 없습니다</div>';
      }
    }

    function displayNotifications(notifications) {
      const list = notificationDropdown.querySelector('.notification-list');
      
      if (notifications.length === 0) {
        list.innerHTML = '<div class="notification-empty">새로운 알림이 없습니다</div>';
        return;
      }

      list.innerHTML = notifications.map(notification => `
        <div class="notification-item ${!notification.read ? 'notification-item--unread' : ''}" 
             data-id="${notification.id}">
          <div class="notification-icon">${getNotificationIcon(notification.type)}</div>
          <div class="notification-content">
            <div class="notification-text">${notification.message}</div>
            <div class="notification-time">${formatTimeAgo(notification.createdAt)}</div>
          </div>
          ${!notification.read ? '<div class="notification-dot"></div>' : ''}
        </div>
      `).join('');

      // Handle notification clicks
      list.addEventListener('click', (e) => {
        const item = e.target.closest('.notification-item');
        if (item) {
          const id = item.dataset.id;
          markNotificationAsRead(id);
          
          // Navigate to notification link if available
          const notification = notifications.find(n => n.id == id);
          if (notification && notification.link) {
            window.location.href = notification.link;
          }
        }
      });
    }

    function getNotificationIcon(type) {
      const icons = {
        comment: '💬',
        vote: '👍',
        mention: '👤',
        system: '🔔'
      };
      return icons[type] || '🔔';
    }

    function formatTimeAgo(dateString) {
      const date = new Date(dateString);
      const now = new Date();
      const diffInMinutes = Math.floor((now - date) / (1000 * 60));
      
      if (diffInMinutes < 1) return '방금 전';
      if (diffInMinutes < 60) return `${diffInMinutes}분 전`;
      
      const diffInHours = Math.floor(diffInMinutes / 60);
      if (diffInHours < 24) return `${diffInHours}시간 전`;
      
      const diffInDays = Math.floor(diffInHours / 24);
      return `${diffInDays}일 전`;
    }

    // Mark notification as read
    window.markNotificationAsRead = async function(id) {
      try {
        await fetch(`/api/notifications/${id}/read`, { method: 'POST' });
        
        // Update UI
        const item = document.querySelector(`[data-id="${id}"]`);
        if (item) {
          item.classList.remove('notification-item--unread');
          const dot = item.querySelector('.notification-dot');
          if (dot) dot.remove();
        }
        
        // Update badge count
        updateNotificationBadge();
      } catch (error) {
        console.error('Failed to mark notification as read:', error);
      }
    };

    // Mark all notifications as read
    window.markAllNotificationsAsRead = async function() {
      try {
        await fetch('/api/notifications/read-all', { method: 'POST' });
        
        // Update UI
        document.querySelectorAll('.notification-item--unread').forEach(item => {
          item.classList.remove('notification-item--unread');
          const dot = item.querySelector('.notification-dot');
          if (dot) dot.remove();
        });
        
        updateNotificationBadge();
      } catch (error) {
        console.error('Failed to mark all notifications as read:', error);
      }
    };

    // Update notification badge
    function updateNotificationBadge() {
      const badge = document.querySelector('.notification-badge');
      const unreadCount = document.querySelectorAll('.notification-item--unread').length;
      
      if (unreadCount > 0) {
        badge.textContent = unreadCount;
        badge.style.display = 'flex';
      } else {
        badge.style.display = 'none';
      }
    }

    // Poll for new notifications every 30 seconds
    setInterval(loadNotifications, 30000);
  }

  // ==========================================================================
  // Coin Price Ticker
  // ==========================================================================

  function initCoinTicker() {
    const tickerElements = document.querySelectorAll('.coin-ticker__item');
    if (tickerElements.length === 0) return;

    // Update coin prices every 10 seconds
    setInterval(async () => {
      try {
        const response = await fetch('/api/crypto/simple-prices');
        const prices = await response.json();
        
        tickerElements.forEach(element => {
          const coinId = element.dataset.coinId;
          if (prices[coinId]) {
            updateCoinElement(element, prices[coinId]);
          }
        });
      } catch (error) {
        console.error('Failed to update coin prices:', error);
      }
    }, 10000);

    function updateCoinElement(element, priceData) {
      const priceElement = element.querySelector('.coin-ticker__price');
      const changeElement = element.querySelector('.coin-ticker__change');
      
      if (priceElement) {
        priceElement.textContent = utils.formatCurrency(priceData.current_price);
      }
      
      if (changeElement && priceData.price_change_percentage_24h !== null) {
        const change = priceData.price_change_percentage_24h;
        changeElement.textContent = utils.formatPercentage(change);
        changeElement.className = `coin-ticker__change ${change >= 0 ? 'up' : 'down'}`;
      }
    }
  }

  // ==========================================================================
  // Social Sharing
  // ==========================================================================

  function initSocialSharing() {
    document.addEventListener('click', (e) => {
      if (e.target.matches('[data-share]')) {
        e.preventDefault();
        const platform = e.target.dataset.share;
        const url = e.target.dataset.url || window.location.href;
        const title = e.target.dataset.title || document.title;
        
        shareToSocial(platform, url, title);
      }
    });
  }

  function shareToSocial(platform, url, title) {
    const encodedUrl = encodeURIComponent(url);
    const encodedTitle = encodeURIComponent(title);
    
    const shareUrls = {
      twitter: `https://twitter.com/intent/tweet?text=${encodedTitle}&url=${encodedUrl}`,
      facebook: `https://www.facebook.com/sharer/sharer.php?u=${encodedUrl}`,
      kakao: `javascript:void(0)` // Kakao SDK would be needed for proper implementation
    };
    
    if (platform === 'copy') {
      utils.copyToClipboard(url);
    } else if (shareUrls[platform]) {
      window.open(shareUrls[platform], '_blank', 'width=600,height=400');
    }
  }

  // ==========================================================================
  // Toast Notifications CSS
  // ==========================================================================

  function addToastStyles() {
    const style = document.createElement('style');
    style.textContent = `
      .toast {
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 12px 16px;
        border-radius: 8px;
        color: white;
        font-weight: 500;
        z-index: 1000;
        transform: translateX(100%);
        transition: transform 0.3s ease;
        max-width: 300px;
        word-wrap: break-word;
      }
      
      .toast--show {
        transform: translateX(0);
      }
      
      .toast--info { background: var(--color-info); }
      .toast--success { background: var(--color-success); }
      .toast--warning { background: var(--color-warning); }
      .toast--danger { background: var(--color-danger); }
      
      .search-suggestions {
        position: absolute;
        top: 100%;
        left: 0;
        right: 0;
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: 8px;
        box-shadow: var(--shadow-lg);
        z-index: 100;
        max-height: 300px;
        overflow-y: auto;
        display: none;
      }
      
      .suggestion-item {
        display: flex;
        align-items: center;
        padding: 12px;
        cursor: pointer;
        border-bottom: 1px solid var(--color-border);
      }
      
      .suggestion-item:hover {
        background: var(--color-surface-hover);
      }
      
      .suggestion-icon {
        margin-right: 12px;
        font-size: 16px;
      }
      
      .suggestion-title {
        font-weight: 500;
        color: var(--color-text-primary);
      }
      
      .suggestion-subtitle {
        font-size: 12px;
        color: var(--color-text-muted);
        margin-top: 2px;
      }
      
      .notification-dropdown {
        position: absolute;
        top: 100%;
        right: 0;
        width: 320px;
        max-height: 400px;
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: 12px;
        box-shadow: var(--shadow-lg);
        z-index: 100;
      }
      
      .notification-header {
        padding: 16px;
        border-bottom: 1px solid var(--color-border);
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
      
      .notification-header h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
      }
      
      .notification-list {
        max-height: 250px;
        overflow-y: auto;
      }
      
      .notification-item {
        display: flex;
        align-items: flex-start;
        padding: 12px 16px;
        cursor: pointer;
        border-bottom: 1px solid var(--color-border);
        position: relative;
      }
      
      .notification-item:hover {
        background: var(--color-surface-hover);
      }
      
      .notification-item--unread {
        background: rgba(var(--color-primary-rgb), 0.05);
      }
      
      .notification-icon {
        margin-right: 12px;
        font-size: 16px;
      }
      
      .notification-text {
        font-size: 14px;
        color: var(--color-text-primary);
        margin-bottom: 4px;
      }
      
      .notification-time {
        font-size: 12px;
        color: var(--color-text-muted);
      }
      
      .notification-dot {
        position: absolute;
        top: 16px;
        right: 16px;
        width: 8px;
        height: 8px;
        background: var(--color-primary);
        border-radius: 50%;
      }
      
      .notification-footer {
        padding: 12px 16px;
        border-top: 1px solid var(--color-border);
        text-align: center;
      }
      
      .notification-loading,
      .notification-error,
      .notification-empty {
        padding: 20px;
        text-align: center;
        color: var(--color-text-muted);
        font-size: 14px;
      }
    `;
    document.head.appendChild(style);
  }

  // ==========================================================================
  // Tab Management
  // ==========================================================================

  function initTabs() {
    const tabs = document.querySelectorAll('.feed-tab');
    const tabContents = document.querySelectorAll('.feed-tab-content');

    tabs.forEach(tab => {
      tab.addEventListener('click', () => {
        const targetTab = tab.dataset.tab;
        
        // Remove active class from all tabs
        tabs.forEach(t => t.classList.remove('active'));
        tabContents.forEach(content => content.style.display = 'none');
        
        // Add active class to clicked tab
        tab.classList.add('active');
        
        // Show corresponding content
        const targetContent = document.getElementById(`${targetTab}-tab`);
        if (targetContent) {
          targetContent.style.display = 'block';
        }
      });
    });
  }

  // ==========================================================================
  // Market Data Refresh
  // ==========================================================================

  window.refreshMarketData = async function() {
    const button = event.target;
    const originalText = button.innerHTML;
    
    // Show loading state
    button.innerHTML = `
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="animate-spin">
        <path d="M21 12a9 9 0 11-6.219-8.56"></path>
      </svg>
      새로고침 중...
    `;
    button.disabled = true;
    
    try {
      // Refresh the page to get latest data
      window.location.reload();
    } catch (error) {
      console.error('Failed to refresh market data:', error);
      utils.showToast('데이터 새로고침에 실패했습니다', 'danger');
      
      // Restore button state
      button.innerHTML = originalText;
      button.disabled = false;
    }
  };

  // ==========================================================================
  // Board Page Functionality
  // ==========================================================================

  function initBoardPage() {
    // View toggle functionality
    const viewToggleBtns = document.querySelectorAll('.view-toggle-btn');
    const postsGrid = document.getElementById('postsGrid');
    
    if (viewToggleBtns.length > 0 && postsGrid) {
      viewToggleBtns.forEach(btn => {
        btn.addEventListener('click', () => {
          const view = btn.dataset.view;
          
          // Update active state
          viewToggleBtns.forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          
          // Update grid layout
          if (view === 'list') {
            postsGrid.classList.add('list-view');
          } else {
            postsGrid.classList.remove('list-view');
          }
          
          // Store preference
          localStorage.setItem('board-view-preference', view);
        });
      });
      
      // Load saved preference
      const savedView = localStorage.getItem('board-view-preference');
      if (savedView) {
        const savedBtn = document.querySelector(`[data-view="${savedView}"]`);
        if (savedBtn) {
          savedBtn.click();
        }
      }
    }
  }

  // Global board functions
  window.sortPosts = function(sortValue) {
    const url = new URL(window.location);
    url.searchParams.set('sort', sortValue);
    window.location.href = url.toString();
  };

  window.searchPosts = utils.debounce(function(query) {
    const url = new URL(window.location);
    if (query.trim()) {
      url.searchParams.set('search', query);
    } else {
      url.searchParams.delete('search');
    }
    window.location.href = url.toString();
  }, 500);

  window.togglePostMenu = function(button) {
    // Close other dropdowns
    document.querySelectorAll('.post-menu-dropdown').forEach(dropdown => {
      if (dropdown !== button.nextElementSibling) {
        dropdown.style.display = 'none';
      }
    });
    
    // Toggle current dropdown
    const dropdown = button.nextElementSibling;
    dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
  };

  window.toggleBookmark = async function(postId) {
    try {
      const response = await fetch(`/api/posts/${postId}/bookmark`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        }
      });
      
      if (response.ok) {
        utils.showToast('북마크에 추가되었습니다', 'success');
      } else {
        utils.showToast('북마크 추가에 실패했습니다', 'danger');
      }
    } catch (error) {
      console.error('Bookmark error:', error);
      utils.showToast('북마크 추가에 실패했습니다', 'danger');
    }
  };

  window.sharePost = function(postId, title) {
    const url = `${window.location.origin}/posts/${postId}`;
    utils.copyToClipboard(url);
  };

  // Close dropdowns when clicking outside
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.post-card-menu')) {
      document.querySelectorAll('.post-menu-dropdown').forEach(dropdown => {
        dropdown.style.display = 'none';
      });
    }
  });

  // ==========================================================================
  // Initialization
  // ==========================================================================

  function init() {
    // Add toast styles
    addToastStyles();
    
    // Initialize all modules
    initDropdowns();
    initSearch();
    initNotifications();
    initCoinTicker();
    initSocialSharing();
    initTabs();
    initBoardPage();
    
    // Add global utils
    window.utils = utils;
    
    console.log('🚀 Crypto Community App initialized');
  }

  // Initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();
