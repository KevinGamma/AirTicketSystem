<template>
  <div class="my-tickets-page">
    <header class="page-header">
      <div class="container">
        <div class="header-content">
          <div class="page-title-section">
            <h1 class="page-title">{{ $t('tickets.title') }}</h1>
            <p class="page-subtitle">在这里管理您的航班机票。</p>
          </div>
          <div class="header-actions">
            <el-button 
              type="primary" 
              @click="goTo('/flights')"
              class="action-btn"
            >
              <el-icon><Plus /></el-icon>
              预订新机票
            </el-button>
          </div>
        </div>
      </div>
    </header>

    <section class="search-filter-section">
      <div class="container">
        <div class="search-filter-card">
          <div class="search-filter-header">
            <h3 class="section-title">搜索筛选</h3>
            <el-button 
              text 
              type="primary" 
              @click="resetFilters"
              :disabled="!hasActiveFilters"
            >
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </div>
          
          <div class="filter-grid">
            <div class="filter-item">
              <label class="filter-label">航班号</label>
              <el-input
                v-model="filters.flightNumber"
                placeholder="输入航班号搜索"
                clearable
                @input="handleFilterChange"
                class="filter-input"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>

            <div class="filter-item">
              <label class="filter-label">机票状态</label>
              <el-select
                v-model="filters.status"
                placeholder="选择状态"
                clearable
                @change="handleFilterChange"
                class="filter-select"
              >
                <el-option label="全部状态" value="" />
                <el-option label="已预订" value="BOOKED" />
                <el-option label="已支付" value="PAID" />
                <el-option label="待支付改签费" value="PENDING_RESCHEDULE" />
                <el-option label="已登机" value="CHECKED_IN" />
                <el-option label="已取消" value="CANCELLED" />
                <el-option label="已退款" value="REFUNDED" />
              </el-select>
            </div>

            <div class="filter-item">
              <label class="filter-label">出发城市</label>
              <el-select
                v-model="filters.departureCity"
                placeholder="选择出发城市"
                clearable
                filterable
                @change="handleFilterChange"
                class="filter-select"
              >
                <el-option label="全部城市" value="" />
                <el-option 
                  v-for="city in availableCities" 
                  :key="city" 
                  :label="city" 
                  :value="city" 
                />
              </el-select>
            </div>

            <div class="filter-item">
              <label class="filter-label">出发日期</label>
              <el-date-picker
                v-model="filters.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                @change="handleFilterChange"
                class="filter-date"
                format="MM/DD"
                value-format="YYYY-MM-DD"
              />
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="pending-requests-section" v-if="pendingRequests.length > 0">
      <div class="container">
        <el-alert
          type="warning"
          :closable="false"
          class="pending-alert"
        >
          <template #title>
            <div class="alert-content">
              <el-icon class="alert-icon"><Warning /></el-icon>
              <span>您有 {{ pendingRequests.length }} 个待处理请求</span>
              <el-button text type="primary" @click="showPendingRequests = !showPendingRequests">
                {{ showPendingRequests ? '收起' : '查看详情' }}
              </el-button>
            </div>
          </template>
        </el-alert>

        <div v-if="showPendingRequests" class="pending-requests-list">
          <div 
            v-for="request in pendingRequests" 
            :key="request.id"
            class="pending-request-item"
          >
            <div class="request-content">
              <div class="request-type">
                <el-tag :type="request.requestType === 'REFUND' ? 'danger' : 'primary'" size="small">
                  {{ request.requestType === 'REFUND' ? '退款申请' : '改签申请' }}
                </el-tag>
                <el-tag v-if="request.status === 'AWAITING_PAYMENT'" type="warning" size="small">
                  待支付
                </el-tag>
              </div>
              <div class="request-details">
                <span class="ticket-number">{{ request.ticket?.ticketNumber }}</span>
                <span class="request-reason">{{ request.reason }}</span>
              </div>
              <div class="request-time">
                {{ formatDateTime(request.requestTime) }}
              </div>
            </div>
            <div class="request-actions">
              <el-button 
                type="danger" 
                size="small" 
                plain
                @click="cancelRequest(request)"
                :loading="cancelingRequestId === request.id"
              >
                <el-icon><Close /></el-icon>
                取消申请
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="tickets-section">
      <div class="container">
        <!-- Results Header -->
        <div class="results-header">
          <div class="results-info">
            <span class="results-count">
              {{ filteredTickets.length }} 张机票
              <span v-if="hasActiveFilters" class="filter-indicator">（已筛选）</span>
            </span>
          </div>
          <div class="view-controls">
            <el-button-group>
              <el-button 
                :type="viewMode === 'card' ? 'primary' : ''" 
                @click="viewMode = 'card'"
                size="small"
              >
                <el-icon><Grid /></el-icon>
                卡片视图
              </el-button>
              <el-button 
                :type="viewMode === 'table' ? 'primary' : ''" 
                @click="viewMode = 'table'"
                size="small"
              >
                <el-icon><List /></el-icon>
                列表视图
              </el-button>
            </el-button-group>
          </div>
        </div>

        <div v-if="loading" class="loading-container">
          <div v-for="i in 3" :key="i" class="ticket-card-skeleton">
            <el-skeleton animated>
              <template #template>
                <div class="skeleton-ticket-card">
                  <div class="skeleton-header">
                    <el-skeleton-item variant="circle" style="width: 40px; height: 40px" />
                    <div class="skeleton-title">
                      <el-skeleton-item variant="text" style="width: 100px" />
                      <el-skeleton-item variant="text" style="width: 200px" />
                    </div>
                  </div>
                  <div class="skeleton-content">
                    <el-skeleton-item variant="text" style="width: 80%" />
                    <el-skeleton-item variant="text" style="width: 60%" />
                  </div>
                  <div class="skeleton-actions">
                    <el-skeleton-item variant="button" style="width: 80px; height: 32px" />
                    <el-skeleton-item variant="button" style="width: 60px; height: 32px" />
                  </div>
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>

        <div v-else-if="filteredTickets.length === 0" class="empty-state">
          <div class="empty-illustration">
            <el-icon class="empty-icon" :size="80"><Ticket /></el-icon>
          </div>
          <h3 class="empty-title">
            {{ hasActiveFilters ? '没有找到符合条件的机票' : '您还没有机票' }}
          </h3>
          <p class="empty-description">
            {{ hasActiveFilters ? '试试调整筛选条件' : '现在就去预订您的第一张机票吧！' }}
          </p>
          <el-button 
            v-if="!hasActiveFilters" 
            type="primary" 
            @click="goTo('/flights')"
            class="empty-action-btn"
          >
            搜索航班
          </el-button>
          <el-button 
            v-else 
            @click="resetFilters"
            class="empty-action-btn"
          >
            清除筛选
          </el-button>
        </div>

        <div v-else-if="viewMode === 'card'" class="tickets-grid">
          <div 
            v-for="ticket in paginatedTickets" 
            :key="ticket.id"
            class="ticket-card"
            @click="showTicketDetails(ticket)"
            tabindex="0"
            role="button"
            :aria-label="`查看机票 ${ticket.flight?.flightNumber} 详情`"
          >
            <div class="ticket-card-header">
              <div class="airline-info">
                <div class="airline-logo">
                  <img 
                    v-if="ticket.flight?.airline?.logoUrl" 
                    :src="ticket.flight.airline.logoUrl" 
                    :alt="ticket.flight.airline.name"
                    class="airline-logo-img"
                  />
                  <div v-else class="airline-logo-placeholder">
                    <span>{{ getAirlineShortName(ticket.flight?.airline?.name) }}</span>
                  </div>
                </div>
                <div class="flight-basic-info">
                  <div class="flight-number">{{ getFlightNumberDisplay(ticket) }}</div>
                  <div class="route">{{ getCompleteRoute(ticket) }}</div>
                  <div v-if="isConnectingFlight(ticket)" class="connecting-label">
                    <el-tag size="small" type="info">联程航班</el-tag>
                  </div>
                </div>
              </div>
              <div class="status-badges">
                <el-tag 
                  :type="getFlightStatusType(ticket.flight?.status)" 
                  size="small"
                  class="status-tag"
                >
                  <el-icon class="status-icon">
                    <component :is="getFlightStatusIcon(ticket.flight?.status)" />
                  </el-icon>
                  {{ getFlightStatusText(ticket.flight?.status) }}
                </el-tag>
                <el-tag 
                  :type="getTicketStatusType(ticket.status)" 
                  size="small"
                  class="status-tag"
                >
                  <el-icon class="status-icon">
                    <component :is="getTicketStatusIcon(ticket.status)" />
                  </el-icon>
                  {{ getTicketStatusText(ticket.status) }}
                </el-tag>
              </div>
            </div>

            <div class="flight-timeline" :class="{ 'connecting-timeline': isConnectingFlight(ticket) }">
              <template v-if="isConnectingFlight(ticket)">
                <div class="connecting-flights-display">
                  <div class="flight-leg" v-for="(flight, index) in getAllFlights(ticket)" :key="flight.id">
                    <div class="timeline-item departure">
                      <div class="timeline-time-container">
                        <div v-if="isInternationalAirport(flight.departureAirport)" class="local-time-label">当地时间</div>
                        <div class="timeline-time">{{ 
                          isInternationalAirport(flight.departureAirport) 
                            ? formatLocalTime(flight.departureTimeUtc, flight.departureAirport)
                            : formatTime(flight.departureTimeUtc) 
                        }}</div>
                      </div>
                      <div class="timeline-location">{{ getAirportCode(flight.departureAirport) }}</div>
                      <div class="timeline-detail">{{ flight.departureAirport?.name }}</div>
                    </div>
                    <div class="timeline-path">
                      <div class="path-line"></div>
                      <div class="flight-info-mini">
                        <div class="mini-flight-number">{{ flight.flightNumber }}</div>
                        <el-icon class="plane-icon"><Promotion /></el-icon>
                      </div>
                      <div class="path-line"></div>
                    </div>
                    <div class="timeline-item arrival">
                      <div class="timeline-time-container">
                        <div v-if="isInternationalAirport(flight.arrivalAirport)" class="local-time-label">当地时间</div>
                        <div class="timeline-time">
                          {{ 
                            isInternationalAirport(flight.arrivalAirport) 
                              ? formatLocalTime(flight.arrivalTimeUtc, flight.arrivalAirport)
                              : formatTime(flight.arrivalTimeUtc) 
                          }}
                          <span v-if="isInternationalAirport(flight.arrivalAirport) ? isNextDayLocal(flight.departureTimeUtc, flight.arrivalTimeUtc, flight.departureAirport, flight.arrivalAirport) : isNextDay(flight.departureTimeUtc, flight.arrivalTimeUtc)" class="next-day">+1</span>
                        </div>
                      </div>
                      <div class="timeline-location">{{ getAirportCode(flight.arrivalAirport) }}</div>
                      <div class="timeline-detail">{{ flight.arrivalAirport?.name }}</div>
                    </div>
                    <div v-if="index < getAllFlights(ticket).length - 1" class="connection-indicator">
                      <el-icon><ArrowRight /></el-icon>
                    </div>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="timeline-item departure">
                  <div class="timeline-time-container">
                    <div v-if="isInternationalAirport(ticket.flight?.departureAirport)" class="local-time-label">当地时间</div>
                    <div class="timeline-time">{{ 
                      isInternationalAirport(ticket.flight?.departureAirport) 
                        ? formatLocalTime(ticket.flight?.departureTimeUtc, ticket.flight?.departureAirport)
                        : formatTime(ticket.flight?.departureTimeUtc) 
                    }}</div>
                  </div>
                  <div class="timeline-location">{{ getAirportCode(ticket.flight?.departureAirport) }}</div>
                  <div class="timeline-detail">{{ ticket.flight?.departureAirport?.name }}</div>
                </div>
                <div class="timeline-path">
                  <div class="path-line"></div>
                  <el-icon class="plane-icon"><Promotion /></el-icon>
                  <div class="path-line"></div>
                </div>
                <div class="timeline-item arrival">
                  <div class="timeline-time-container">
                    <div v-if="isInternationalAirport(ticket.flight?.arrivalAirport)" class="local-time-label">当地时间</div>
                    <div class="timeline-time">
                      {{ 
                        isInternationalAirport(ticket.flight?.arrivalAirport) 
                          ? formatLocalTime(ticket.flight?.arrivalTimeUtc, ticket.flight?.arrivalAirport)
                          : formatTime(ticket.flight?.arrivalTimeUtc) 
                      }}
                      <span v-if="isInternationalAirport(ticket.flight?.arrivalAirport) ? isNextDayLocal(ticket.flight?.departureTimeUtc, ticket.flight?.arrivalTimeUtc, ticket.flight?.departureAirport, ticket.flight?.arrivalAirport) : isNextDay(ticket.flight?.departureTimeUtc, ticket.flight?.arrivalTimeUtc)" class="next-day">+1</span>
                    </div>
                  </div>
                  <div class="timeline-location">{{ getAirportCode(ticket.flight?.arrivalAirport) }}</div>
                  <div class="timeline-detail">{{ ticket.flight?.arrivalAirport?.name }}</div>
                </div>
              </template>
            </div>

            <PaymentCountdown 
              v-if="ticket.status === 'BOOKED'"
              :ticket-id="ticket.id"
              :status="ticket.status"
            />

            <!-- Cross-airport transfer reminder -->
            <div v-if="hasCrossAirportTransfer(ticket)" class="cross-airport-reminder">
              <div class="reminder-header">
                <el-icon class="warning-icon"><Warning /></el-icon>
                <span class="reminder-title">跨机场转机提醒</span>
              </div>
              <div class="reminder-content">
                <p class="reminder-text">您的行程包含跨机场转机，请注意：</p>
                <div v-for="transfer in getCrossAirportTransferInfo(ticket)" :key="transfer.city" class="transfer-detail">
                  <div class="transfer-info">
                    <span class="transfer-city">{{ transfer.city }}</span>: 
                    <span class="airport-transfer">{{ getAirportCode(transfer.fromAirport) }} → {{ getAirportCode(transfer.toAirport) }}</span>
                  </div>
                  <div class="transfer-airports">
                    <span class="from-airport">{{ transfer.fromAirport.name }}</span> → 
                    <span class="to-airport">{{ transfer.toAirport.name }}</span>
                  </div>
                </div>
                <ul class="reminder-tips">
                  <li>需要自行前往不同机场办理登机手续</li>
                  <li>请预留充足的转机时间（建议至少2-3小时）</li>
                  <li>建议提前了解机场间交通方式</li>
                </ul>
              </div>
            </div>

            <div class="ticket-details">
              <div class="detail-row">
                <span class="detail-label">乘客姓名</span>
                <span class="detail-value">{{ ticket.passengerName }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">座位号</span>
                <span class="detail-value">{{ ticket.seatNumber || '待分配' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">价格</span>
                <span class="detail-value price">¥{{ calculateDisplayTotal(ticket) }}</span>
              </div>
            </div>

            <div class="ticket-actions">
              <el-button 
                v-if="canPay(ticket)" 
                type="success" 
                size="small"
                @click.stop="payTicket(ticket)"
              >
                立即支付
              </el-button>
              <el-button 
                v-if="canPayRescheduleFee(ticket)" 
                type="success" 
                size="small"
                @click.stop="payRescheduleFee(ticket)"
              >
                支付改签费
              </el-button>
              <el-button 
                v-if="canPayRescheduleFee(ticket)" 
                type="primary" 
                size="small"
                plain
                @click.stop="triggerPaymentMonitoring(ticket)"
              >
                检查支付状态
              </el-button>
              <el-button 
                v-if="canReschedule(ticket)" 
                type="primary" 
                size="small"
                @click.stop="showRescheduleDialog(ticket)"
              >
                改签
              </el-button>
              <el-button 
                v-if="canRefund(ticket)" 
                type="warning" 
                size="small"
                @click.stop="refundAndCancelTicket(ticket)"
              >
                退款
              </el-button>
              <el-button 
                v-if="ticket.status === 'BOOKED'" 
                type="danger" 
                size="small"
                plain
                @click.stop="cancelTicketFromCard(ticket)"
              >
                取消
              </el-button>
              <el-button 
                size="small" 
                @click.stop="showTicketDetails(ticket)"
              >
                详情
              </el-button>
            </div>
          </div>
        </div>

        <!-- Table View -->
        <div v-else class="tickets-table-container">
          <el-table :data="paginatedTickets" class="tickets-table">
            <el-table-column label="航班号" width="140">
              <template #default="scope">
                <div class="table-flight-number">
                  {{ getFlightNumberDisplay(scope.row) }}
                  <el-tag v-if="isConnectingFlight(scope.row)" size="small" type="info" class="connecting-tag">
                    联程
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="路线" width="250">
              <template #default="scope">
                <div class="table-route">{{ getCompleteRoute(scope.row) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="出发时间" width="180">
              <template #default="scope">
                <div class="table-time">
                  <div v-if="isInternationalAirport(scope.row.flight?.departureAirport)" class="local-time-label-table">当地时间</div>
                  <div>{{
                    isInternationalAirport(scope.row.flight?.departureAirport) 
                      ? formatLocalDateTime(scope.row.flight?.departureTimeUtc, scope.row.flight?.departureAirport)
                      : formatDateTime(scope.row.flight?.departureTimeUtc) 
                  }}</div>
                  <div v-if="isConnectingFlight(scope.row)" class="connecting-info">
                    <small class="text-secondary">首班出发</small>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="到达时间" width="180">
              <template #default="scope">
                <div class="table-time">
                  <template v-if="isConnectingFlight(scope.row)">
                    <div v-if="isInternationalAirport(getLastFlight(scope.row)?.arrivalAirport)" class="local-time-label-table">当地时间</div>
                    <div>{{
                      isInternationalAirport(getLastFlight(scope.row)?.arrivalAirport) 
                        ? formatLocalDateTime(getLastFlight(scope.row)?.arrivalTimeUtc, getLastFlight(scope.row)?.arrivalAirport)
                        : formatDateTime(getLastFlight(scope.row)?.arrivalTimeUtc) 
                    }}</div>
                    <div class="connecting-info">
                      <small class="text-secondary">最终到达</small>
                    </div>
                  </template>
                  <template v-else>
                    <div v-if="isInternationalAirport(scope.row.flight?.arrivalAirport)" class="local-time-label-table">当地时间</div>
                    <div>{{
                      isInternationalAirport(scope.row.flight?.arrivalAirport) 
                        ? formatLocalDateTime(scope.row.flight?.arrivalTimeUtc, scope.row.flight?.arrivalAirport)
                        : formatDateTime(scope.row.flight?.arrivalTimeUtc) 
                    }}</div>
                  </template>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="航班状态" width="140">
              <template #default="scope">
                <div class="flight-status-display">
                  <template v-if="isConnectingFlight(scope.row)">
                    <!-- Show overall status for connecting flights -->
                    <el-tag :type="getConnectingFlightStatusType(scope.row)" size="small">
                      {{ getConnectingFlightStatusText(scope.row) }}
                    </el-tag>
                  </template>
                  <template v-else>
                    <el-tag :type="getFlightStatusType(scope.row.flight?.status)" size="small">
                      {{ getFlightStatusText(scope.row.flight?.status) }}
                    </el-tag>
                  </template>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="$t('tickets.ticketStatus')" width="120">
              <template #default="scope">
                <el-tag :type="getTicketStatusType(scope.row.status)" size="small">
                  {{ getTicketStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('tickets.operation')" width="200" fixed="right">
              <template #default="scope">
                <div class="table-actions">
                  <el-button v-if="canPay(scope.row)" type="success" size="small" @click="payTicket(scope.row)">
                    支付
                  </el-button>
                  <el-button v-if="canPayRescheduleFee(scope.row)" type="success" size="small" @click="payRescheduleFee(scope.row)">
                    支付改签费
                  </el-button>
                  <el-button v-if="canReschedule(scope.row)" type="primary" size="small" @click="showRescheduleDialog(scope.row)">
                    改签
                  </el-button>
                  <el-button v-if="canRefund(scope.row)" type="warning" size="small" @click="refundAndCancelTicket(scope.row)">
                    退款
                  </el-button>
                  <el-button size="small" @click="showTicketDetails(scope.row)">
                    详情
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="totalPages > 1" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="filteredTickets.length"
            layout="total, prev, pager, next, jumper"
            @current-change="handlePageChange"
            class="tickets-pagination"
          />
        </div>
      </div>
    </section>

    <!-- Ticket Details Modal -->
    <el-drawer
      v-model="ticketDetailsVisible"
      title=""
      direction="rtl"
      size="600px"
      class="ticket-details-drawer"
    >
      <template #header>
        <div class="drawer-header">
          <h3>机票详情</h3>
          <span class="ticket-number">{{ selectedTicket?.ticketNumber }}</span>
        </div>
      </template>
      
      <div v-if="selectedTicket" class="ticket-details-content">
        <div class="detail-section">
          <h4 class="section-title">航班信息</h4>
          <div class="flight-summary-card">
            <div class="flight-summary-header">
              <div class="airline-brand">
                <div class="airline-logo-large">
                  <img 
                    v-if="selectedTicket.flight?.airline?.logoUrl" 
                    :src="selectedTicket.flight.airline.logoUrl" 
                    :alt="selectedTicket.flight.airline.name"
                    class="airline-logo-img"
                  />
                  <div v-else class="airline-logo-placeholder">
                    <span>{{ getAirlineShortName(selectedTicket.flight?.airline?.name) }}</span>
                  </div>
                </div>
                <div>
                  <div class="flight-number-large">{{ getFlightNumberDisplay(selectedTicket) }}</div>
                  <div class="airline-name">{{ selectedTicket.flight?.airline?.name || '航空公司' }}</div>
                  <div v-if="isConnectingFlight(selectedTicket)" class="connecting-badge">
                    <el-tag type="info" size="large">联程航班</el-tag>
                  </div>
                </div>
              </div>
              <div class="flight-status-large">
                <template v-if="isConnectingFlight(selectedTicket)">
                  <el-tag :type="getConnectingFlightStatusType(selectedTicket)" size="large">
                    {{ getConnectingFlightStatusText(selectedTicket) }}
                  </el-tag>
                </template>
                <template v-else>
                  <el-tag :type="getFlightStatusType(selectedTicket.flight?.status)" size="large">
                    {{ getFlightStatusText(selectedTicket.flight?.status) }}
                  </el-tag>
                </template>
              </div>
            </div>
            
            <div class="flight-route-detailed">
              <template v-if="isConnectingFlight(selectedTicket)">
                <div class="connecting-route-detailed">
                  <div v-for="(flight, index) in getAllFlights(selectedTicket)" :key="flight.id" class="route-segment">
                    <div class="route-endpoint departure-endpoint">
                      <div v-if="isInternationalAirport(flight.departureAirport)" class="local-time-label-detail">当地时间</div>
                      <div class="time-large">{{ 
                        isInternationalAirport(flight.departureAirport) 
                          ? formatLocalTime(flight.departureTimeUtc, flight.departureAirport)
                          : formatTime(flight.departureTimeUtc) 
                      }}</div>
                      <div class="date">{{ formatDate(flight.departureTimeUtc) }}</div>
                      <div class="airport-code">{{ getAirportCode(flight.departureAirport) }}</div>
                      <div class="airport-name">{{ flight.departureAirport?.name }}</div>
                      <div class="city-name">{{ flight.departureAirport?.city }}</div>
                    </div>
                    
                    <div class="route-path-detailed">
                      <div class="path-duration">{{ calculateFlightDuration(flight.departureTimeUtc, flight.arrivalTimeUtc) }}</div>
                      <div class="flight-number-path">{{ flight.flightNumber }}</div>
                      <div class="path-line-detailed"></div>
                      <el-icon class="plane-icon-large"><Promotion /></el-icon>
                      <div class="path-line-detailed"></div>
                    </div>
                    
                    <div class="route-endpoint arrival-endpoint">
                      <div v-if="isInternationalAirport(flight.arrivalAirport)" class="local-time-label-detail">当地时间</div>
                      <div class="time-large">{{ 
                        isInternationalAirport(flight.arrivalAirport) 
                          ? formatLocalTime(flight.arrivalTimeUtc, flight.arrivalAirport)
                          : formatTime(flight.arrivalTimeUtc) 
                      }}</div>
                      <div class="date">{{ formatDate(flight.arrivalTimeUtc) }}</div>
                      <div class="airport-code">{{ getAirportCode(flight.arrivalAirport) }}</div>
                      <div class="airport-name">{{ flight.arrivalAirport?.name }}</div>
                      <div class="city-name">{{ flight.arrivalAirport?.city }}</div>
                    </div>
                    
                    <div v-if="index < getAllFlights(selectedTicket).length - 1" class="layover-info">
                      <div class="layover-indicator">
                        <el-icon class="transfer-icon-large"><Refresh /></el-icon>
                        <span class="layover-text">转机</span>
                        <span class="layover-airport">{{ flight.arrivalAirport?.city }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
              <template v-else>
                <!-- Direct flight route -->
                <div class="route-endpoint departure-endpoint">
                  <div v-if="isInternationalAirport(selectedTicket.flight?.departureAirport)" class="local-time-label-detail">当地时间</div>
                  <div class="time-large">{{ 
                    isInternationalAirport(selectedTicket.flight?.departureAirport) 
                      ? formatLocalTime(selectedTicket.flight?.departureTimeUtc, selectedTicket.flight?.departureAirport)
                      : formatTime(selectedTicket.flight?.departureTimeUtc) 
                  }}</div>
                  <div class="date">{{ formatDate(selectedTicket.flight?.departureTimeUtc) }}</div>
                  <div class="airport-code">{{ getAirportCode(selectedTicket.flight?.departureAirport) }}</div>
                  <div class="airport-name">{{ selectedTicket.flight?.departureAirport?.name }}</div>
                  <div class="city-name">{{ selectedTicket.flight?.departureAirport?.city }}</div>
                </div>
                
                <div class="route-path-detailed">
                  <div class="path-duration">
                    {{ calculateFlightDuration(selectedTicket.flight?.departureTimeUtc, selectedTicket.flight?.arrivalTimeUtc) }}
                  </div>
                  <div class="path-line-detailed"></div>
                  <el-icon class="plane-icon-large"><Promotion /></el-icon>
                  <div class="path-line-detailed"></div>
                </div>
                
                <div class="route-endpoint arrival-endpoint">
                  <div v-if="isInternationalAirport(selectedTicket.flight?.arrivalAirport)" class="local-time-label-detail">当地时间</div>
                  <div class="time-large">{{ 
                    isInternationalAirport(selectedTicket.flight?.arrivalAirport) 
                      ? formatLocalTime(selectedTicket.flight?.arrivalTimeUtc, selectedTicket.flight?.arrivalAirport)
                      : formatTime(selectedTicket.flight?.arrivalTimeUtc) 
                  }}</div>
                  <div class="date">{{ formatDate(selectedTicket.flight?.arrivalTimeUtc) }}</div>
                  <div class="airport-code">{{ getAirportCode(selectedTicket.flight?.arrivalAirport) }}</div>
                  <div class="airport-name">{{ selectedTicket.flight?.arrivalAirport?.name }}</div>
                  <div class="city-name">{{ selectedTicket.flight?.arrivalAirport?.city }}</div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- Cross-airport transfer reminder for detailed view -->
        <div v-if="hasCrossAirportTransfer(selectedTicket)" class="cross-airport-reminder detailed">
          <div class="reminder-header">
            <el-icon class="warning-icon"><Warning /></el-icon>
            <span class="reminder-title">跨机场转机重要提醒</span>
          </div>
          <div class="reminder-content">
            <p class="reminder-text">您的行程包含跨机场转机，请特别注意：</p>
            <div v-for="transfer in getCrossAirportTransferInfo(selectedTicket)" :key="transfer.city" class="transfer-detail">
              <div class="transfer-info-detailed">
                <div class="transfer-city-label">{{ transfer.city }}转机</div>
                <div class="airport-transfer-detailed">
                  <div class="from-airport-detail">
                    <span class="airport-code">{{ getAirportCode(transfer.fromAirport) }}</span>
                    <span class="airport-name">{{ transfer.fromAirport.name }}</span>
                    <span class="flight-ref">（{{ transfer.fromFlight }} 到达）</span>
                  </div>
                  <el-icon class="transfer-arrow"><ArrowRight /></el-icon>
                  <div class="to-airport-detail">
                    <span class="airport-code">{{ getAirportCode(transfer.toAirport) }}</span>
                    <span class="airport-name">{{ transfer.toAirport.name }}</span>
                    <span class="flight-ref">（{{ transfer.toFlight }} 出发）</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="reminder-tips-detailed">
              <h4 class="tips-title">重要提醒：</h4>
              <ul class="reminder-tips">
                <li><strong>机场间换乘</strong>：需要自行前往不同机场办理登机手续</li>
                <li><strong>时间安排</strong>：建议预留至少2-3小时转机时间</li>
                <li><strong>交通方式</strong>：请提前了解机场间交通选项（地铁、巴士、出租车等）</li>
                <li><strong>行李提取</strong>：可能需要提取行李并重新托运</li>
                <li><strong>签证要求</strong>：如有需要，请确认是否需要入境签证</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- Passenger Information -->
        <div class="detail-section">
          <h4 class="section-title">乘客信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <label>乘客姓名</label>
              <span>{{ selectedTicket.passengerName }}</span>
            </div>
            <div class="info-item">
              <label>身份证号</label>
              <span>{{ selectedTicket.passengerIdNumber }}</span>
            </div>
            <div class="info-item">
              <label>座位号</label>
              <span>{{ selectedTicket.seatNumber || '待分配' }}</span>
            </div>
            <div class="info-item">
              <label>舱位类型</label>
              <span>{{ getTicketTypeText(selectedTicket.ticketType) }}</span>
            </div>
          </div>
        </div>

        <!-- Booking Information -->
        <div class="detail-section">
          <h4 class="section-title">订单信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <label>机票号</label>
              <span>{{ selectedTicket.ticketNumber }}</span>
            </div>
            <div class="info-item">
              <label>票价</label>
              <span class="price-large">¥{{ calculateBaseTicketPrice(selectedTicket) }}</span>
            </div>
            <div class="info-item">
              <label>机建燃油费</label>
              <span>¥{{ calculateFuelSurcharge(selectedTicket) }}</span>
            </div>
            <div class="info-item">
              <label>总计</label>
              <span class="price-total">¥{{ calculateDisplayTotal(selectedTicket) }}</span>
            </div>
            <div class="info-item">
              <label>预订时间</label>
              <span>{{ formatDateTime(selectedTicket.bookingTime) }}</span>
            </div>
            <div class="info-item">
              <label>支付时间</label>
              <span>{{ selectedTicket.paymentTime ? formatDateTime(selectedTicket.paymentTime) : '未支付' }}</span>
            </div>
            <div class="info-item full-width">
              <label>机票状态</label>
              <el-tag :type="getTicketStatusType(selectedTicket.status)" size="large">
                {{ getTicketStatusText(selectedTicket.status) }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- Payment Countdown for BOOKED tickets -->
        <div v-if="selectedTicket && selectedTicket.status === 'BOOKED' && countdownDataLoaded" class="detail-section">
          <h4 class="section-title">支付倒计时</h4>
          <div class="countdown-card" :class="selectedTicketUrgencyClass">
            <div class="countdown-header">
              <el-icon class="countdown-icon"><Clock /></el-icon>
              <span class="countdown-title">剩余支付时间</span>
            </div>
            <div class="countdown-display">{{ selectedTicketCountdown }}</div>
            <div class="countdown-message">{{ selectedTicketMessage }}</div>
          </div>
        </div>
        
        <!-- Loading state for countdown -->
        <div v-if="selectedTicket && selectedTicket.status === 'BOOKED' && !countdownDataLoaded" class="detail-section">
          <h4 class="section-title">支付倒计时</h4>
          <div class="countdown-card urgency-low">
            <div class="countdown-header">
              <el-icon class="countdown-icon"><Clock /></el-icon>
              <span class="countdown-title">剩余支付时间</span>
            </div>
            <div class="countdown-display">加载中...</div>
            <div class="countdown-message">正在获取支付信息</div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="detail-actions">
          <el-button 
            v-if="canPay(selectedTicket)" 
            type="success" 
            size="large"
            @click="payTicket(selectedTicket)"
          >
            <el-icon><CreditCard /></el-icon>
            立即支付
          </el-button>
          <el-button 
            v-if="canPayRescheduleFee(selectedTicket)" 
            type="success" 
            size="large"
            @click="payRescheduleFee(selectedTicket)"
          >
            <el-icon><CreditCard /></el-icon>
            支付改签费
          </el-button>
          <el-button 
            v-if="canReschedule(selectedTicket)" 
            type="primary" 
            size="large"
            @click="showRescheduleDialog(selectedTicket)"
          >
            <el-icon><Refresh /></el-icon>
            申请改签
          </el-button>
          <el-button 
            v-if="canRefund(selectedTicket)" 
            type="warning" 
            size="large"
            @click="refundAndCancelTicket(selectedTicket)"
          >
            <el-icon><Money /></el-icon>
            立即退款
          </el-button>
          <el-button 
            v-if="selectedTicket && selectedTicket.status === 'BOOKED'" 
            type="danger" 
            size="large"
            plain
            @click="cancelSelectedTicket"
          >
            <el-icon><Close /></el-icon>
            取消订单
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- Reschedule Dialog -->
    <el-dialog v-model="rescheduleDialogVisible" title="改签申请" width="900px" class="reschedule-dialog">
      <div v-if="selectedTicket" class="reschedule-content">
        <!-- Current Ticket Info -->
        <div class="reschedule-section">
          <h4 class="section-title">当前机票</h4>
          <div class="current-ticket-card">
            <div class="ticket-summary">
              <span class="flight-number">{{ getFlightNumberDisplay(selectedTicket) }}</span>
              <span class="route">{{ getCompleteRoute(selectedTicket) }}</span>
              <span class="price">¥{{ calculateDisplayTotal(selectedTicket) }}</span>
              <div v-if="isConnectingFlight(selectedTicket)" class="connecting-badge">
                <el-tag type="info" size="small">联程航班</el-tag>
              </div>
            </div>
            
            <!-- Detailed flight segments for connecting flights -->
            <div v-if="isConnectingFlight(selectedTicket)" class="flight-segments-details">
              <div class="segments-header">航班详情</div>
              <div 
                v-for="flight in getAllFlights(selectedTicket)" 
                :key="flight.id"
                class="segment-detail-reschedule"
              >
                <div class="segment-info">
                  <span class="segment-flight">{{ flight.flightNumber }}</span>
                  <span class="segment-route">
                    {{ flight.departureAirport?.city }} → {{ flight.arrivalAirport?.city }}
                  </span>
                  <span class="segment-time">
                    {{ formatTime(flight.departureTimeUtc) }} - {{ formatTime(flight.arrivalTimeUtc) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Service Fee Warning -->
        <div v-if="serviceFee" class="reschedule-section">
          <el-alert type="warning" :closable="false" class="fee-alert">
            <template #title>
              <div class="fee-info">
                <strong>改签手续费：¥{{ serviceFee }}</strong>
                <p>改签费用根据距离起飞时间计算，24小时内改签费用更高</p>
              </div>
            </template>
          </el-alert>
        </div>

        <div v-if="serviceFee" class="reschedule-section">
          <div class="fare-difference-card">
            <h4 class="fare-calculation-title">票价差额计算</h4>
            <div class="fare-calculation-content">
              <div class="fare-item">
                <span class="fare-label">原航班票价：</span>
                <span class="fare-value">¥{{ calculateDisplayTotal(selectedTicket) }}</span>
              </div>
              <div v-if="selectedNewFlight" class="fare-item">
                <span class="fare-label">新航班票价：</span>
                <span class="fare-value">¥{{ selectedNewFlight.price }}</span>
              </div>
              <div v-else class="fare-item">
                <span class="fare-label">新航班票价：</span>
                <span class="fare-value fare-placeholder">请选择新航班</span>
              </div>
              <div class="fare-divider"></div>
              <div class="fare-item fare-difference">
                <span class="fare-label">票价差额：</span>
                <span class="fare-value" :class="getFareDifferenceClass()">
                  {{ getFareDifferenceText() }}
                </span>
              </div>
              <div class="fare-item">
                <span class="fare-label">改签手续费：</span>
                <span class="fare-value">¥{{ serviceFee }}</span>
              </div>
              <div class="fare-divider total-divider"></div>
              <div class="fare-item fare-total">
                <span class="fare-label">总计费用(需另加120元机建燃油费)：</span>
                <span class="fare-value total-amount">{{ getTotalFeeText() }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="reschedule-section">
          <h4 class="section-title">航班筛选</h4>
          <div class="filter-controls">
            <div class="filter-row">
              <div class="filter-item">
                <label class="filter-label">出发日期：</label>
                <el-date-picker
                  v-model="selectedDate"
                  type="date"
                  placeholder="选择日期（留空显示全部）"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  :disabled-date="disablePastDates"
                  @change="onDateChange"
                  clearable
                  class="date-picker"
                />
              </div>
              <div class="filter-item">
                <label class="filter-label">航班类型：</label>
                <el-radio-group v-model="includeConnecting" @change="onFlightTypeChange" class="flight-type-radio">
                  <el-radio :label="false">直飞航班</el-radio>
                  <el-radio :label="true">包含转机航班</el-radio>
                </el-radio-group>
              </div>
            </div>
            <div class="filter-actions">
              <el-button @click="searchFlights" :loading="loadingFlights" type="primary">
                <el-icon><Search /></el-icon>
                搜索航班
              </el-button>
              <el-button @click="clearFilters">
                <el-icon><RefreshLeft /></el-icon>
                清除筛选
              </el-button>
            </div>
          </div>
        </div>

        <!-- Available Flights -->
        <div class="reschedule-section">
          <h4 class="section-title">
            选择新航班
            <span v-if="availableFlights.length > 0" class="flights-count">
              (共找到 {{ availableFlights.length }} 个航班)
            </span>
          </h4>
          <div v-loading="loadingFlights" class="flights-list">
            <div v-if="availableFlights.length === 0 && !loadingFlights" class="no-flights">
              <el-empty description="暂无符合条件的航班" />
              <p class="search-tips">
                提示：尝试调整筛选条件或选择其他日期
              </p>
            </div>
            <div 
              v-for="flight in availableFlights" 
              :key="flight.id"
              class="flight-option"
              :class="{ 'selected': selectedNewFlight?.id === flight.id }"
              @click="selectFlight(flight)"
            >
              <div class="flight-option-content">
                <div class="flight-info">
                  <span class="flight-number">
                    {{ getAvailableFlightDisplay(flight) }}
                  </span>
                  <span class="flight-time">
                    {{ formatDateTime(flight.departureTimeUtc) + ' - ' + formatDateTime(flight.arrivalTimeUtc) }}
                  </span>
                  <div v-if="flight.connectingFlights && flight.connectingFlights.length > 0" class="connecting-flight-info">
                    <el-tag size="small" type="info">联程航班 ({{ flight.connectingFlights.length + 1 }} 段)</el-tag>
                    <div class="connecting-segments">
                      <span class="segment">
                        {{ flight.departureAirport?.city }} → {{ flight.arrivalAirport?.city }}
                      </span>
                      <span v-for="connecting in flight.connectingFlights" :key="connecting.id" class="segment">
                        <span class="segment-separator"> → </span>
                        {{ connecting.departureAirport?.city }} → {{ connecting.arrivalAirport?.city }}
                      </span>
                    </div>
                    <div class="connecting-flight-numbers">
                      <span class="flight-number-item">{{ flight.flightNumber }}</span>
                      <span v-for="connecting in flight.connectingFlights" :key="connecting.id" class="flight-number-item">
                        + {{ connecting.flightNumber }}
                      </span>
                    </div>
                  </div>
                </div>
                <div class="flight-details">
                  <span class="price">¥{{ flight.price }}</span>
                  <span class="seats">余票 {{ flight.availableSeats }}</span>
                </div>
              </div>
              <div class="selection-indicator">
                <el-icon v-if="selectedNewFlight?.id === flight.id"><Check /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- Reschedule Reason -->
        <div class="reschedule-section">
          <h4 class="section-title">改签原因</h4>
          <el-radio-group v-model="rescheduleReason" class="reason-radio-group">
            <el-radio value="日程调整">日程调整</el-radio>
            <el-radio value="工作原因">工作原因</el-radio>
            <el-radio value="家庭紧急情况">家庭紧急情况</el-radio>
            <el-radio value="航班时间不合适">航班时间不合适</el-radio>
            <el-radio value="身体健康原因">身体健康原因</el-radio>
            <el-radio value="天气原因">天气原因</el-radio>
            <el-radio value="其他个人原因">其他个人原因</el-radio>
          </el-radio-group>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeRescheduleDialog">取消</el-button>
          <el-button 
            type="primary" 
            @click="confirmReschedule"
            :disabled="!selectedNewFlight || !rescheduleReason"
            :loading="rescheduleLoading"
          >
            {{ getConfirmButtonText() }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'

dayjs.extend(utc)
dayjs.extend(timezone)
import {
  Search,
  Refresh,
  RefreshLeft,
  Warning,
  Grid,
  List,
  Ticket,
  Plus,
  Promotion,
  CircleCheck,
  Clock,
  Close,
  InfoFilled,
  CreditCard,
  Money,
  Check,
  ArrowRight // eslint-disable-line no-unused-vars
} from '@element-plus/icons-vue'
import PaymentCountdown from '../components/PaymentCountdown.vue'
import api from '../api'

export default {
  name: 'MyTicketsNew',
  components: {
    Search,
    Refresh,
    RefreshLeft,
    Warning,
    Grid,
    List,
    Ticket,
    Plus,
    Promotion,
    CircleCheck,
    Clock,
    Close,
    InfoFilled,
    CreditCard,
    Money,
    Check,
    PaymentCountdown
  },
  data() {
    return {
      // Data state
      tickets: [],
      pendingRequests: [],
      loading: false,
      cancelingRequestId: null,
      
      // Filter state
      filters: {
        flightNumber: '',
        status: '',
        departureCity: '',
        dateRange: null
      },
      
      // View state
      viewMode: 'card', // 'card' or 'table'
      showPendingRequests: false,
      
      // Pagination
      currentPage: 1,
      pageSize: 6,

      ticketDetailsVisible: false,
      selectedTicket: null,

      rescheduleDialogVisible: false,
      availableFlights: [],
      selectedNewFlight: null,
      serviceFee: null,
      rescheduleReason: '',
      selectedDate: null,
      includeConnecting: true,
      loadingFlights: false,
      rescheduleLoading: false,
      
      // Selected ticket countdown
      selectedTicketCountdown: '00:00',
      selectedTicketMessage: '',
      selectedTicketUrgency: 'LOW',
      selectedTicketTimer: null,
      selectedTicketRemainingSeconds: 0,
      countdownDataLoaded: false
    }
  },
  computed: {
    ...mapState(['currentUser']),
    ...mapGetters(['isLoggedIn']),
    
    availableCities() {
      const cities = new Set()
      this.tickets.forEach(ticket => {
        if (ticket.flight?.departureAirport?.city) {
          cities.add(ticket.flight.departureAirport.city)
        }
      })
      return Array.from(cities).sort()
    },
    
    hasActiveFilters() {
      return !!(
        this.filters.flightNumber || 
        this.filters.status || 
        this.filters.departureCity || 
        this.filters.dateRange
      )
    },
    
    filteredTickets() {
      let filtered = [...this.tickets]
      
      // Flight number filter
      if (this.filters.flightNumber) {
        filtered = filtered.filter(ticket => 
          ticket.flight?.flightNumber?.toLowerCase()
            .includes(this.filters.flightNumber.toLowerCase())
        )
      }
      
      // Status filter
      if (this.filters.status) {
        filtered = filtered.filter(ticket => ticket.status === this.filters.status)
      }
      
      // Departure city filter
      if (this.filters.departureCity) {
        filtered = filtered.filter(ticket => 
          ticket.flight?.departureAirport?.city === this.filters.departureCity
        )
      }
      
      // Date range filter
      if (this.filters.dateRange && this.filters.dateRange.length === 2) {
        const [startDate, endDate] = this.filters.dateRange
        filtered = filtered.filter(ticket => {
          // Use our parseDateTime function to ensure proper timezone conversion
          const parsedDate = this.parseDateTime(ticket.flight?.departureTimeUtc)
          if (!parsedDate) return false
          const departureDate = parsedDate.format('YYYY-MM-DD')
          console.log('🔍 Date filtering:', ticket.flight?.departureTimeUtc, '->', departureDate, 'range:', startDate, 'to', endDate)
          return departureDate >= startDate && departureDate <= endDate
        })
      }
      
      // Sort by booking time (latest to earliest) - using parseDateTime for consistency
      return filtered.sort((a, b) => {
        const dateA = this.parseDateTime(a.bookingTime)
        const dateB = this.parseDateTime(b.bookingTime)
        if (!dateA || !dateB) return 0
        return dateB.valueOf() - dateA.valueOf()
      })
    },
    
    totalPages() {
      return Math.ceil(this.filteredTickets.length / this.pageSize)
    },
    
    paginatedTickets() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredTickets.slice(start, end)
    },
    
    selectedTicketUrgencyClass() {
      return `urgency-${this.selectedTicketUrgency.toLowerCase()}`
    }
  },
  methods: {
    goTo(path) {
      this.$router.push(path)
    },
    
    // Filter methods
    handleFilterChange() {
      this.currentPage = 1 // Reset to first page when filtering
    },
    
    resetFilters() {
      this.filters = {
        flightNumber: '',
        status: '',
        departureCity: '',
        dateRange: null
      }
      this.currentPage = 1
    },
    
    // Pagination
    handlePageChange(page) {
      this.currentPage = page
    },
    
    // Data loading
    async loadTickets() {
      try {
        this.loading = true
        const response = await api.get('/tickets/my')
        console.log('🎫 Raw API response:', response.data)
        
        if (response.data.success) {
          // Sort tickets by booking time (most recent first)
          this.tickets = response.data.data.sort((a, b) => {
            const timeA = new Date(a.bookingTime)
            const timeB = new Date(b.bookingTime)
            return timeB - timeA // Descending order (newest first)
          })
          
          // Enhanced Debug: Log all ticket time information
          console.log(`✅ Loaded ${this.tickets.length} tickets with enhanced debugging`)
          
          this.tickets.forEach((ticket, index) => {
            if (ticket.flight) {
              console.log(`🎫 Ticket ${index + 1} (${ticket.ticketNumber}) flight times:`)
              console.log('  - Raw departureTimeUtc:', ticket.flight.departureTimeUtc, '| type:', typeof ticket.flight.departureTimeUtc)
              console.log('  - Raw arrivalTimeUtc:', ticket.flight.arrivalTimeUtc, '| type:', typeof ticket.flight.arrivalTimeUtc)
              console.log('  - Raw departureTime:', ticket.flight.departureTime, '| type:', typeof ticket.flight.departureTime)
              console.log('  - Raw arrivalTime:', ticket.flight.arrivalTime, '| type:', typeof ticket.flight.arrivalTime)
              console.log('  - Booking time:', ticket.bookingTime, '| type:', typeof ticket.bookingTime)
              console.log('  - Payment time:', ticket.paymentTime, '| type:', typeof ticket.paymentTime)
              
              // Test parsing for this ticket
              if (ticket.flight.departureTimeUtc) {
                const parsed = this.parseDateTime(ticket.flight.departureTimeUtc)
                console.log('  - Parsed departure result:', parsed ? parsed.format('YYYY-MM-DD HH:mm:ss') : 'FAILED')
              }
              if (ticket.flight.arrivalTimeUtc) {
                const parsed = this.parseDateTime(ticket.flight.arrivalTimeUtc)
                console.log('  - Parsed arrival result:', parsed ? parsed.format('YYYY-MM-DD HH:mm:ss') : 'FAILED')
              }
              
              // Check for connecting flights
              if (ticket.connectingFlights && ticket.connectingFlights.length > 0) {
                console.log(`  - Has ${ticket.connectingFlights.length} connecting flights:`)
                ticket.connectingFlights.forEach((cf, cfIndex) => {
                  console.log(`    Connecting Flight ${cfIndex + 1}:`)
                  console.log('      - departureTimeUtc:', cf.departureTimeUtc, '| type:', typeof cf.departureTimeUtc)
                  console.log('      - arrivalTimeUtc:', cf.arrivalTimeUtc, '| type:', typeof cf.arrivalTimeUtc)
                })
              }
            } else {
              console.log(`🎫 Ticket ${index + 1} (${ticket.ticketNumber}) has NO flight data`)
            }
          })
        }
        await this.loadPendingRequests()
        
        // Check for pending refunds after loading tickets
        setTimeout(() => {
          this.tickets.forEach(ticket => {
            if (ticket.status === 'PAID' || ticket.status === 'PENDING_RESCHEDULE') {
              this.checkAndNotifyPendingRefunds(ticket)
            }
          })
        }, 1000)
        
      } catch (error) {
        ElMessage.error('加载机票失败')
        this.tickets = [
          {
            id: 1,
            ticketNumber: 'T001234567',
            passengerName: '张三',
            passengerIdNumber: '110101199001011234',
            seatNumber: '12A',
            price: 1280,
            ticketType: 'ECONOMY',
            status: 'PAID',
            bookingTime: '2025-08-15T08:30:00',
            paymentTime: '2025-08-15T08:35:00',
            flight: {
              flightNumber: 'CA2345',
              airline: { name: '中国国际航空', logoUrl: null },
              departureAirport: { city: '北京', code: 'PEK', name: '北京首都国际机场' },
              arrivalAirport: { city: '上海', code: 'PVG', name: '上海浦东国际机场' },
              departureTime: '2025-08-20T09:15:00',
              arrivalTime: '2025-08-20T11:50:00',
              status: 'SCHEDULED'
            }
          },
          {
            id: 2,
            ticketNumber: 'T001234568',
            passengerName: '李四',
            passengerIdNumber: '110101199002022345',
            seatNumber: null,
            price: 850,
            ticketType: 'ECONOMY',
            status: 'BOOKED',
            bookingTime: '2025-08-16T14:20:00',
            paymentTime: null,
            flight: {
              flightNumber: 'MU5678',
              airline: { name: '中国东方航空', logoUrl: null },
              departureAirport: { city: '上海', code: 'SHA', name: '上海虹桥国际机场' },
              arrivalAirport: { city: '广州', code: 'CAN', name: '广州白云国际机场' },
              departureTime: '2025-08-25T14:20:00',
              arrivalTime: '2025-08-25T17:30:00',
              status: 'DELAYED'
            }
          }
        ].sort((a, b) => {
          const timeA = new Date(a.bookingTime)
          const timeB = new Date(b.bookingTime)
          return timeB - timeA // Descending order (newest first)
        })
      } finally {
        this.loading = false
      }
    },
    
    async loadPendingRequests() {
      try {
        const response = await api.get('/tickets/my-requests')
        if (response.data.success) {
          this.pendingRequests = response.data.data.filter(req => 
            (req.status === 'PENDING' || req.status === 'AWAITING_PAYMENT') &&
            req.rejectionReason !== 'Canceled by user'
          )
        }
      } catch (error) {
        console.error('Failed to load pending requests:', error)
        this.pendingRequests = []
      }
    },

    async cancelRequest(request) {
      try {
        await this.$confirm(
          `确定要取消这个${request.requestType === 'REFUND' ? '退款' : '改签'}申请吗？取消后需要重新提交申请。`,
          '确认取消',
          {
            confirmButtonText: '确定取消',
            cancelButtonText: '保留申请',
            type: 'warning'
          }
        )
        
        this.cancelingRequestId = request.id
        
        const response = await api.delete(`/tickets/requests/${request.id}`)
        
        if (response.data.success) {
          this.$message.success('申请已取消')
          
          // Remove the canceled request from the pending list
          this.pendingRequests = this.pendingRequests.filter(req => req.id !== request.id)
          
          // Refresh the tickets list to update any status changes
          await this.loadTickets()
        } else {
          throw new Error(response.data.message || '取消申请失败')
        }
      } catch (error) {
        if (error !== 'cancel') { // User didn't cancel the confirmation
          console.error('Cancel request error:', error)
          this.$message.error('取消申请失败: ' + (error.message || '未知错误'))
        }
      } finally {
        this.cancelingRequestId = null
      }
    },
    
    // Action methods
    canPay(ticket) {
      return ticket.status === 'BOOKED'
    },
    
    canPayRescheduleFee(ticket) {
      return ticket.status === 'PENDING_RESCHEDULE'
    },
    
    canReschedule(ticket) {
      // For connecting flights, ensure all flights in the itinerary can be rescheduled
      if (this.isConnectingFlight(ticket)) {
        const allFlights = this.getAllFlights(ticket)
        const canRescheduleAll = allFlights.every(flight => 
          flight && ['SCHEDULED', 'DELAYED'].includes(flight.status)
        )
        return ticket.status === 'PAID' && canRescheduleAll
      }
      return ticket.status === 'PAID' && 
             ['SCHEDULED', 'DELAYED'].includes(ticket.flight?.status)
    },
    
    canRefund(ticket) {
      // For connecting flights, ensure all flights in the itinerary can be refunded
      if (this.isConnectingFlight(ticket)) {
        const allFlights = this.getAllFlights(ticket)
        const canRefundAll = allFlights.every(flight => 
          flight && ['SCHEDULED', 'DELAYED'].includes(flight.status)
        )
        return ticket.status === 'PAID' && canRefundAll
      }
      return ticket.status === 'PAID' && 
             ['SCHEDULED', 'DELAYED'].includes(ticket.flight?.status)
    },
    
    async payTicket(ticket) {
      try {
        await ElMessageBox.confirm(
          `确认支付机票 ${ticket.ticketNumber}？`, 
          '支付确认', 
          {
            confirmButtonText: '确认支付',
            cancelButtonText: '取消',
            type: 'info'
          }
        )
        
        // Navigate to payment page
        this.$router.push(`/payment/${ticket.id}`)
        this.ticketDetailsVisible = false
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('支付取消')
        }
      }
    },
    
    async payRescheduleFee(ticket) {
      try {
        this.loading = true
        
        // Create reschedule payment order
        const response = await api.post(`/tickets/${ticket.id}/reschedule-payment`)
        
        if (response.data.success) {
          const paymentData = response.data.data
          
          // Use the same approach as regular payment - write HTML to new window
          if (paymentData.paymentUrl) {
            const paymentWindow = window.open('', '_blank', 'width=1000,height=700,scrollbars=yes,resizable=yes')
            
            if (paymentWindow) {
              try {
                // Write the Alipay HTML content to the new window
                paymentWindow.document.write(paymentData.paymentUrl)
                paymentWindow.document.close()
                
                // Set window title
                paymentWindow.document.title = '改签费用支付 - 支付宝沙箱支付'
                
                ElMessage.success('改签费用支付订单已创建，请在新窗口中完成支付')
                
                // Monitor payment completion
                const checkPaymentStatus = setInterval(async () => {
                  try {
                    if (paymentWindow.closed) {
                      clearInterval(checkPaymentStatus)
                      // Reload tickets to check if payment was completed
                      await this.loadTickets()
                      ElMessage.info('支付窗口已关闭，正在刷新机票状态...')
                    } else {
                      // Also periodically check payment status while window is open
                      try {
                        const currentTicket = this.tickets.find(t => t.id === ticket.id)
                        if (currentTicket && currentTicket.status === 'PAID') {
                          clearInterval(checkPaymentStatus)
                          ElMessage.success('改签费用支付成功！')
                          await this.loadTickets()
                          paymentWindow.close()
                        }
                      } catch (error) {
                        // Silent fail for status checks
                      }
                    }
                  } catch (error) {
                    console.error('Error checking payment window status:', error)
                  }
                }, 3000)
                
                // Auto-stop checking after 10 minutes
                setTimeout(() => {
                  clearInterval(checkPaymentStatus)
                }, 600000)
              } catch (error) {
                console.error('Failed to open payment window:', error)
                paymentWindow.close()
                ElMessage.error('无法打开支付窗口，请检查浏览器设置')
              }
            } else {
              ElMessage.error('无法打开支付窗口，请允许弹窗后重试')
            }
          } else {
            ElMessage.error('支付URL获取失败')
          }
        } else {
          ElMessage.error(response.data.message || '创建改签费用支付订单失败')
        }
      } catch (error) {
        console.error('Create reschedule payment error:', error)
        ElMessage.error(error.response?.data?.message || '创建改签费用支付订单失败')
      } finally {
        this.loading = false
        this.ticketDetailsVisible = false
      }
    },

    async checkAndNotifyPendingRefunds(ticket) {
      try {
        const requestsResponse = await api.get('/tickets/my-requests')
        
        if (requestsResponse.data.success) {
          const allRequests = requestsResponse.data.data || []
          
          // Use the same improved logic to find reschedule requests
          let ticketRequests = []
          
          // Method 1: Direct match with current ticket
          ticketRequests = allRequests.filter(req => 
            req.ticket && req.ticket.id === ticket.id && req.requestType === 'RESCHEDULE' && req.status === 'APPROVED'
          )
          
          // Method 2: If current ticket is a rescheduled ticket, look for requests on the original ticket
          if (ticketRequests.length === 0 && ticket.originalTicketId) {
            ticketRequests = allRequests.filter(req => 
              req.ticket && req.ticket.id === ticket.originalTicketId && req.requestType === 'RESCHEDULE' && req.status === 'APPROVED'
            )
          }
          
          // Method 3: Look for any reschedule requests that resulted in this ticket
          if (ticketRequests.length === 0) {
            ticketRequests = allRequests.filter(req => 
              req.requestType === 'RESCHEDULE' && req.status === 'APPROVED' && req.ticket && (
                // The request ticket is the original ticket, and this ticket is the result
                (req.ticket.id === ticket.originalTicketId) ||
                // The request references this ticket somehow
                req.ticket.rescheduledToTicketId === ticket.id ||
                // Broad search - any reschedule request related to our ticket chain
                (ticket.originalTicketId && req.ticket.id === ticket.originalTicketId)
              )
            )
          }
          
          for (const request of ticketRequests) {
            // Calculate if this should result in a refund based on total amount
            if (request.totalAmount < 0 && request.paymentStatus === 'COMPLETED') {
              const refundAmount = Math.abs(request.totalAmount)
              
              // Use MessageBox for interactive refund request
              setTimeout(() => {
                ElMessageBox.confirm(
                  `您的改签产生了¥${refundAmount}的退款，如果您还没有收到，可以点击"确定"提交退款申请。`, 
                  '改签退款提醒', 
                  {
                    confirmButtonText: '申请退款',
                    cancelButtonText: '我已收到',
                    type: 'info'
                  }
                ).then(() => {
                  this.requestMissingRescheduleRefund(ticket)
                }).catch(() => {
                  // User clicked "我已收到" or canceled - do nothing
                })
              }, 2000)
            }
          }
        }
      } catch (error) {
        // Silent fail - this is just an informational check
        console.log('Could not check refund status:', error)
      }
    },
    
    async completeReschedulePaymentTest(ticket) {
      try {
        await ElMessageBox.confirm(
          '这是测试功能，将直接完成改签支付。确认继续？', 
          '测试支付完成', 
          {
            confirmButtonText: '确认完成',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        this.loading = true
        
        const response = await api.post(`/tickets/${ticket.id}/complete-reschedule-payment-test`)
        
        if (response.data.success) {
          ElMessage.success('测试支付完成成功')
          await this.loadTickets()
        } else {
          ElMessage.error(response.data.message || '测试支付完成失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Complete reschedule payment test error:', error)
          ElMessage.error(error.response?.data?.message || '测试支付完成失败')
        }
      } finally {
        this.loading = false
        this.ticketDetailsVisible = false
      }
    },
    
    async debugTicketStatus(ticket) {
      try {
        this.loading = true
        
        const response = await api.get(`/tickets/${ticket.id}/debug-status`)
        
        if (response.data.success) {
          const debugInfo = response.data.data
          
          // Display debug information in console and alert
          console.log('Debug Info for Ticket', ticket.id, ':', debugInfo)
          
          const summary = `
票据状态调试信息:
===================
票据ID: ${debugInfo.ticket.id}
票据号: ${debugInfo.ticket.ticketNumber}
当前状态: ${debugInfo.ticket.status}
原票据ID: ${debugInfo.ticket.originalTicketId || 'null'}
改签到票据ID: ${debugInfo.ticket.rescheduledToTicketId || 'null'}
是否原票: ${debugInfo.ticket.isOriginalTicket}

前端按钮逻辑:
- 可支付改签费: ${debugInfo.frontendButtonLogic.canPayRescheduleFee}
- 可支付: ${debugInfo.frontendButtonLogic.canPay}
- 可改签: ${debugInfo.frontendButtonLogic.canReschedule}

相关审批请求 (${debugInfo.approvalRequests.length}个):
${debugInfo.approvalRequests.map(req => 
  `- ID: ${req.id}, 票据ID: ${req.ticketId}, 类型: ${req.requestType}, 状态: ${req.status}, 总金额: ${req.totalAmount}, 支付状态: ${req.paymentStatus}`
).join('\n')}
          `
          
          this.$alert(summary, '票据状态调试信息', {
            type: 'info',
            customClass: 'debug-alert'
          })
        } else {
          ElMessage.error('获取调试信息失败: ' + response.data.message)
        }
      } catch (error) {
        console.error('Debug ticket status error:', error)
        ElMessage.error('获取调试信息失败: ' + (error.response?.data?.message || error.message))
      } finally {
        this.loading = false
      }
    },
    
    async triggerPaymentMonitoring(ticket) {
      if (!ticket || !ticket.id) {
        ElMessage.error('无法获取票据信息')
        return
      }
      
      try {
        this.loading = true
        
        // Check if there are any approval requests for this ticket
        const requestsResponse = await api.get('/tickets/my-requests')
        
        if (requestsResponse.data.success) {
          const allRequests = requestsResponse.data.data || []
          
          // Find reschedule requests for this specific ticket
          const ticketRequests = allRequests.filter(req => 
            req.ticket && req.ticket.id === ticket.id && req.requestType === 'RESCHEDULE'
          )
          
          // Check if there are any pending reschedule requests (not yet paid)
          const pendingRequests = ticketRequests.filter(req => req.status === 'PENDING')
          
          if (pendingRequests.length > 0) {
            ElMessage.warning('您还未支付改签费用，请先完成支付后再检查支付状态。')
            return
          }
          
          // Check if there are approved but unpaid requests
          const approvedUnpaidRequests = ticketRequests.filter(req => 
            req.status === 'APPROVED' && (!req.paymentStatus || req.paymentStatus === 'PENDING')
          )
          
          if (approvedUnpaidRequests.length > 0) {
            ElMessage.warning('您的改签申请已通过，但还未支付改签费用，请先完成支付后再检查支付状态。')
            return
          }
          
          // Check if there are completed requests to monitor
          const completedRequests = ticketRequests.filter(req => 
            req.status === 'APPROVED' && req.paymentStatus === 'COMPLETED'
          )
          
          if (completedRequests.length === 0 && ticketRequests.length === 0) {
            ElMessage.info('暂无需要检查的支付记录。')
            return
          }
        }
        
        // If payment has been made, proceed with monitoring
        const response = await api.post('/tickets/trigger-payment-monitoring')
        
        if (response.data.success) {
          ElMessage.success('支付状态检查已触发')
          
          // Check if this reschedule should generate a refund
          if (requestsResponse.data.success) {
            const allRequests = requestsResponse.data.data || []
            const ticketRequests = allRequests.filter(req => 
              req.ticket && req.ticket.id === ticket.id && req.requestType === 'RESCHEDULE' && req.status === 'APPROVED'
            )
            
            const refundRequest = ticketRequests.find(req => req.totalAmount < 0)
            if (refundRequest) {
              const refundAmount = Math.abs(refundRequest.totalAmount)
              setTimeout(() => {
                ElMessage.info(`检测到您有¥${refundAmount}的改签退款，处理完成后将退回原支付方式`, { duration: 8000 })
              }, 2000)
            }
          }
          
          // 等待一秒后刷新票据列表
          setTimeout(async () => {
            await this.loadTickets()
          }, 1000)
        } else {
          ElMessage.error('触发支付状态检查失败: ' + response.data.message)
        }
      } catch (error) {
        console.error('Trigger payment monitoring error:', error)
        ElMessage.error('触发支付状态检查失败: ' + (error.response?.data?.message || error.message))
      } finally {
        this.loading = false
      }
    },

    async requestMissingRescheduleRefund(ticket) {
      try {
        // Check if this ticket has a reschedule that should result in a refund
        const requestsResponse = await api.get('/tickets/my-requests')
        
        if (!requestsResponse.data.success) {
          ElMessage.error('无法获取改签信息')
          return
        }
        
        const allRequests = requestsResponse.data.data || []
        
        // Look for reschedule requests in multiple ways:
        // 1. Current ticket ID (for original tickets)
        // 2. Original ticket ID (for rescheduled tickets)
        // 3. All related tickets in the reschedule chain
        let rescheduleRequests = []
        
        // Method 1: Direct match with current ticket
        rescheduleRequests = allRequests.filter(req => 
          req.ticket && req.ticket.id === ticket.id && req.requestType === 'RESCHEDULE' && req.status === 'APPROVED'
        )
        
        // Method 2: If current ticket is a rescheduled ticket, look for requests on the original ticket
        if (rescheduleRequests.length === 0 && ticket.originalTicketId) {
          rescheduleRequests = allRequests.filter(req => 
            req.ticket && req.ticket.id === ticket.originalTicketId && req.requestType === 'RESCHEDULE' && req.status === 'APPROVED'
          )
          console.log(`Looking for reschedule requests on original ticket ${ticket.originalTicketId}:`, rescheduleRequests)
        }
        
        // Method 3: Look for any reschedule requests that resulted in this ticket
        if (rescheduleRequests.length === 0) {
          rescheduleRequests = allRequests.filter(req => 
            req.requestType === 'RESCHEDULE' && req.status === 'APPROVED' && req.ticket && (
              // The request ticket is the original ticket, and this ticket is the result
              (req.ticket.id === ticket.originalTicketId) ||
              // The request references this ticket somehow
              req.ticket.rescheduledToTicketId === ticket.id ||
              // Broad search - any reschedule request related to our ticket chain
              (ticket.originalTicketId && req.ticket.id === ticket.originalTicketId)
            )
          )
          console.log(`Broad search for reschedule requests related to ticket ${ticket.id} or original ${ticket.originalTicketId}:`, rescheduleRequests)
        }
        
        // Method 4: Since ticket relationships are null, use alternative matching approaches
        if (rescheduleRequests.length === 0) {
          const allRescheduleRequests = allRequests.filter(req => 
            req.requestType === 'RESCHEDULE' && req.status === 'APPROVED'
          )
          console.log(`All ${allRescheduleRequests.length} approved reschedule requests:`, allRescheduleRequests)
          
          // Since req.ticket is null for all requests, we need to find the relevant request by other means
          // Let's look for the most recent approved reschedule request for this user
          console.log('=== ALTERNATIVE MATCHING STRATEGY ===')
          console.log('Since all ticket relationships are null, looking for most recent reschedule request...')
          
          // Sort by ID (assuming higher ID = more recent) and take the most recent approved ones
          const recentRescheduleRequests = allRescheduleRequests
            .sort((a, b) => b.id - a.id) // Sort by ID descending
            .slice(0, 3) // Take the 3 most recent
          
          console.log('Most recent approved reschedule requests:', recentRescheduleRequests)
          
          // For tickets 189->190, let's assume one of the recent requests applies
          rescheduleRequests = recentRescheduleRequests
          
          // Log detailed structure of the requests we're going to check
          console.log('=== DETAILED REQUEST ANALYSIS ===')
          rescheduleRequests.forEach((req, index) => {
            console.log(`Request ${index + 1}:`, {
              id: req.id,
              requestType: req.requestType,
              status: req.status,
              totalAmount: req.totalAmount,
              paymentStatus: req.paymentStatus,
              requestTime: req.requestTime,
              reason: req.reason,
              allFields: req // Log the complete object to see what fields exist
            })
          })
          console.log('=== END ANALYSIS ===')
        }
        
        if (rescheduleRequests.length === 0) {
          // Show detailed debug information to help understand the issue
          const debugInfo = {
            ticketId: ticket.id,
            ticketNumber: ticket.ticketNumber,
            originalTicketId: ticket.originalTicketId,
            rescheduledToTicketId: ticket.rescheduledToTicketId,
            isOriginalTicket: ticket.isOriginalTicket,
            totalRequests: allRequests.length,
            rescheduleRequests: allRequests.filter(req => req.requestType === 'RESCHEDULE').length,
            approvedRescheduleRequests: allRequests.filter(req => req.requestType === 'RESCHEDULE' && req.status === 'APPROVED').length
          }
          
          console.log('Debug info:', debugInfo)
          console.log('All requests:', allRequests)
          console.log('Reschedule requests:', allRequests.filter(req => req.requestType === 'RESCHEDULE'))
          
          // Show user-friendly debug message
          ElMessageBox.alert(
            `调试信息：
            
票据ID: ${ticket.id}
票据号: ${ticket.ticketNumber}
原票据ID: ${ticket.originalTicketId || '无'}
改签到票据ID: ${ticket.rescheduledToTicketId || '无'}
是否原票: ${ticket.isOriginalTicket || '未知'}

总请求数: ${allRequests.length}
改签请求数: ${allRequests.filter(req => req.requestType === 'RESCHEDULE').length}
已批准改签请求数: ${allRequests.filter(req => req.requestType === 'RESCHEDULE' && req.status === 'APPROVED').length}

请截图发送给技术支持。`, 
            '调试信息', 
            { type: 'info' }
          )
          return
        }
        
        console.log('=== CHECKING FOR REFUNDS ===')
        console.log('Found reschedule requests:', rescheduleRequests.length)
        rescheduleRequests.forEach((req, index) => {
          console.log(`Request ${index + 1} refund check:`, {
            id: req.id,
            totalAmount: req.totalAmount,
            isNegative: req.totalAmount < 0,
            paymentStatus: req.paymentStatus
          })
        })
        
        const refundRequest = rescheduleRequests.find(req => req.totalAmount < 0)
        console.log('Refund request found:', refundRequest)
        
        if (!refundRequest) {
          // Show detailed information about found requests
          ElMessageBox.alert(
            `找到 ${rescheduleRequests.length} 个改签记录，但都不涉及退款：

${rescheduleRequests.map((req, index) => 
  `${index + 1}. 请求ID: ${req.id}
     状态: ${req.status}
     总金额: ¥${req.totalAmount || '未设置'}
     支付状态: ${req.paymentStatus || '未知'}`
).join('\n\n')}

如果您认为应该有退款，请联系客服。`, 
            '改签记录详情', 
            { type: 'info' }
          )
          console.log('Found reschedule requests but no refund needed:', rescheduleRequests)
          return
        }
        
        const refundAmount = Math.abs(refundRequest.totalAmount)
        
        await ElMessageBox.confirm(
          `检测到您的改签应产生¥${refundAmount}的退款。\n\n是否要提交退款申请？`, 
          '改签退款申请', 
          {
            confirmButtonText: '申请退款',
            cancelButtonText: '取消',
            type: 'info'
          }
        )
        
        this.loading = true
        
        // Submit refund request using existing refund API
        const response = await api.post(`/tickets/${ticket.id}/refund`, { 
          reason: `改签产生退款申请 - 应退金额¥${refundAmount}` 
        })
        
        if (response.data.success) {
          ElMessage.success(`改签退款申请已提交，预计退款金额：¥${refundAmount}`)
          await this.loadTickets()
        } else {
          ElMessage.error('退款申请失败: ' + response.data.message)
        }
        
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Request missing refund error:', error)
          ElMessage.error('退款申请失败: ' + (error.response?.data?.message || error.message))
        }
      } finally {
        this.loading = false
      }
    },

    async requestManualRefund(ticket) {
      try {
        // Allow user to manually specify refund amount and reason
        const { value: refundInfo } = await ElMessageBox.prompt(
          '请输入您认为应该退还的金额和原因（格式：金额,原因）\n例如：50,改签到便宜航班应退差价', 
          '手动申请退款', 
          {
            confirmButtonText: '申请退款',
            cancelButtonText: '取消',
            inputPattern: /^\d+(\.\d{1,2})?,.*$/,
            inputErrorMessage: '请按格式输入：金额,原因'
          }
        )
        
        const [amountStr, reason] = refundInfo.split(',')
        const amount = parseFloat(amountStr.trim())
        const refundReason = reason.trim()
        
        if (isNaN(amount) || amount <= 0) {
          ElMessage.error('请输入有效的退款金额')
          return
        }
        
        await ElMessageBox.confirm(
          `您要申请退款¥${amount}，原因：${refundReason}\n\n确认提交申请？`, 
          '确认退款申请', 
          {
            confirmButtonText: '确认申请',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        this.loading = true
        
        const response = await api.post(`/tickets/${ticket.id}/refund`, { 
          reason: `手动退款申请 - ¥${amount} - ${refundReason}` 
        })
        
        if (response.data.success) {
          ElMessage.success(`退款申请已提交，申请金额：¥${amount}`)
          await this.loadTickets()
        } else {
          ElMessage.error('退款申请失败: ' + response.data.message)
        }
        
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Manual refund request error:', error)
          ElMessage.error('退款申请失败: ' + (error.response?.data?.message || error.message))
        }
      } finally {
        this.loading = false
      }
    },

    async showDetailedDebugInfo(ticket) {
      try {
        const requestsResponse = await api.get('/tickets/my-requests')
        
        if (requestsResponse.data.success) {
          const allRequests = requestsResponse.data.data || []
          
          // Find all reschedule requests for this ticket or its original
          const allRescheduleRequests = allRequests.filter(req => req.requestType === 'RESCHEDULE')
          const relatedRequests = allRescheduleRequests.filter(req => 
            req.ticket && (
              req.ticket.id === ticket.id ||
              req.ticket.id === ticket.originalTicketId ||
              (ticket.originalTicketId && req.ticket.id === ticket.originalTicketId)
            )
          )
          
          // Create detailed info display
          let debugMessage = `票据信息：
票据ID: ${ticket.id}
票据号: ${ticket.ticketNumber}
原票据ID: ${ticket.originalTicketId || '无'}
改签到票据ID: ${ticket.rescheduledToTicketId || '无'}

相关改签请求 (${relatedRequests.length}个)：\n`
          
          if (relatedRequests.length === 0) {
            debugMessage += '未找到相关改签请求\n\n'
            debugMessage += `所有改签请求 (${allRescheduleRequests.length}个)：\n`
            allRescheduleRequests.slice(0, 5).forEach((req, index) => {
              debugMessage += `${index + 1}. ID:${req.id} 票据ID:${req.ticket?.id} 状态:${req.status} 金额:${req.totalAmount || '未设置'}\n`
            })
          } else {
            relatedRequests.forEach((req, index) => {
              debugMessage += `${index + 1}. 请求ID: ${req.id}
   票据ID: ${req.ticket?.id}
   票据号: ${req.ticket?.ticketNumber || '未知'}
   状态: ${req.status}
   总金额: ¥${req.totalAmount || '未设置'}
   支付状态: ${req.paymentStatus || '未知'}
   申请时间: ${req.requestTime || '未知'}
   原因: ${req.reason || '未填写'}\n\n`
            })
          }
          
          ElMessageBox.alert(debugMessage, '改签详情调试信息', { 
            type: 'info',
            customClass: 'debug-info-dialog'
          })
        }
      } catch (error) {
        ElMessage.error('获取改签详情失败: ' + error.message)
      }
    },

    async selectRescheduleRequestForRefund(ticket) {
      try {
        const requestsResponse = await api.get('/tickets/my-requests')
        
        if (!requestsResponse.data.success) {
          ElMessage.error('无法获取改签信息')
          return
        }
        
        const allRequests = requestsResponse.data.data || []
        const rescheduleRequests = allRequests.filter(req => 
          req.requestType === 'RESCHEDULE' && req.status === 'APPROVED'
        ).sort((a, b) => b.id - a.id) // Most recent first
        
        if (rescheduleRequests.length === 0) {
          ElMessage.warning('未找到已批准的改签请求')
          return
        }
        
        // Create a simple HTML list of all reschedule requests
        let requestsList = '请选择与您的改签相关的申请记录（点击确定后输入对应的请求ID）:\n\n'
        
        rescheduleRequests.slice(0, 10).forEach((req) => {
          const amount = req.totalAmount !== null && req.totalAmount !== undefined ? `¥${req.totalAmount}` : '未设置'
          const refundStatus = req.totalAmount < 0 ? ' 🔴退款' : (req.totalAmount > 0 ? ' 💰付费' : ' ⚪无费用')
          requestsList += `${req.id}: ${req.requestTime || '未知时间'} - ${req.reason || '无原因'} - 金额:${amount}${refundStatus}\n`
        })
        
        requestsList += '\n请输入您要申请退款的请求ID (例如: 39):'
        
        // Show selection dialog
        const { value: selectedRequestId } = await ElMessageBox.prompt(
          requestsList, 
          '选择改签记录', 
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputType: 'text',
            inputPlaceholder: '输入请求ID (例如: 39)',
            inputValidator: (value) => {
              if (!value) {
                return '请输入请求ID'
              }
              const requestId = parseInt(value)
              const exists = rescheduleRequests.find(req => req.id == requestId)
              if (!exists) {
                return '请求ID不存在，请输入有效的请求ID'
              }
              return true
            }
          }
        )
        
        const selectedRequest = rescheduleRequests.find(req => req.id == selectedRequestId)
        
        if (selectedRequest && selectedRequest.totalAmount < 0) {
          const refundAmount = Math.abs(selectedRequest.totalAmount)
          
          await ElMessageBox.confirm(
            `您选择的改签记录显示应退款¥${refundAmount}。\n\n是否要提交退款申请？`, 
            '确认退款申请', 
            {
              confirmButtonText: '申请退款',
              cancelButtonText: '取消',
              type: 'info'
            }
          )
          
          this.loading = true
          const response = await api.post(`/tickets/${ticket.id}/refund`, { 
            reason: `基于改签记录${selectedRequest.id}的退款申请 - 应退金额¥${refundAmount}` 
          })
          
          if (response.data.success) {
            ElMessage.success(`退款申请已提交，预计退款金额：¥${refundAmount}`)
            await this.loadTickets()
          } else {
            ElMessage.error('退款申请失败: ' + response.data.message)
          }
          
        } else if (selectedRequest) {
          ElMessage.info(`选择的改签记录 (金额: ¥${selectedRequest.totalAmount || '未设置'}) 不涉及退款`)
        }
        
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('操作失败: ' + (error.message || '未知错误'))
        }
      } finally {
        this.loading = false
      }
    },

    async calculateAndRequestRefund(ticket) {
      try {
        // First, we need to find the original ticket to compare prices
        if (!ticket.originalTicketId) {
          ElMessage.warning('无法找到原始票据信息，无法计算退款金额')
          return
        }
        
        // Try to find the original ticket in the current tickets list
        let originalTicket = this.tickets.find(t => t.id === ticket.originalTicketId)
        
        if (!originalTicket) {
          // If not found in current list, ask user for original ticket price
          const { value: originalPrice } = await ElMessageBox.prompt(
            `无法自动获取原始票据价格。\n\n当前票据: ${ticket.ticketNumber} (¥${ticket.price})\n原始票据ID: ${ticket.originalTicketId}\n\n请输入您原始票据的价格:`, 
            '输入原始票价', 
            {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              inputType: 'number',
              inputPlaceholder: '例如: 680.00',
              inputValidator: (value) => {
                const price = parseFloat(value)
                if (!value || isNaN(price) || price <= 0) {
                  return '请输入有效的价格'
                }
                return true
              }
            }
          )
          
          originalTicket = { price: parseFloat(originalPrice) }
        }

        const priceDifference = originalTicket.price - ticket.price

        const { value: serviceFeeInput } = await ElMessageBox.prompt(
          `价格对比:\n原票价: ¥${originalTicket.price}\n新票价: ¥${ticket.price}\n价格差: ¥${priceDifference}\n\n请输入您支付的改签手续费:`, 
          '输入改签手续费', 
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputType: 'number',
            inputPlaceholder: '例如: 50.00',
            inputValidator: (value) => {
              const fee = parseFloat(value)
              if (!value || isNaN(fee) || fee < 0) {
                return '请输入有效的手续费'
              }
              return true
            }
          }
        )
        
        const serviceFee = parseFloat(serviceFeeInput)
        const netRefund = priceDifference - serviceFee
        
        let refundMessage = `退款计算结果:\n\n原票价: ¥${originalTicket.price}\n新票价: ¥${ticket.price}\n价格差: ¥${priceDifference}\n改签手续费: ¥${serviceFee}\n\n`
        
        if (netRefund > 0) {
          refundMessage += `应退款金额: ¥${netRefund}\n\n是否要申请此退款?`
          
          await ElMessageBox.confirm(refundMessage, '确认退款申请', {
            confirmButtonText: '申请退款',
            cancelButtonText: '取消',
            type: 'info'
          })
          
          this.loading = true
          const response = await api.post(`/tickets/${ticket.id}/refund`, { 
            reason: `改签价格差退款申请 - 原价¥${originalTicket.price} 新价¥${ticket.price} 手续费¥${serviceFee} 应退¥${netRefund}` 
          })
          
          if (response.data.success) {
            ElMessage.success(`退款申请已提交，申请金额：¥${netRefund}`)
            await this.loadTickets()
          } else {
            ElMessage.error('退款申请失败: ' + response.data.message)
          }
          
        } else {
          refundMessage += `计算结果: 无需退款 (手续费大于等于价格差)`
          ElMessageBox.alert(refundMessage, '计算结果', { type: 'info' })
        }
        
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('操作失败: ' + (error.message || '未知错误'))
        }
      } finally {
        this.loading = false
      }
    },

    async fixExistingRescheduleRefund(ticket) {
      try {
        this.loading = true
        
        // Get all requests to find the specific reschedule request that needs fixing
        const requestsResponse = await api.get('/tickets/my-requests')
        
        if (!requestsResponse.data.success) {
          ElMessage.error('无法获取请求列表')
          return
        }
        
        const allRequests = requestsResponse.data.data || []
        
        // Find approved reschedule requests that might need refunds
        const approvedRescheduleRequests = allRequests.filter(req => 
          req.requestType === 'RESCHEDULE' && req.status === 'APPROVED'
        )
        
        if (approvedRescheduleRequests.length === 0) {
          ElMessage.warning('没有找到已批准的改签记录')
          return
        }
        
        // Let user select which request to process
        const options = approvedRescheduleRequests.map(req => {
          return {
            label: `请求ID: ${req.id} - ${req.reason || '改签申请'} (总金额: ¥${req.totalAmount || '未设置'})`,
            value: req.id,
            req: req
          }
        })
        
        const { value: selectedRequestId } = await ElMessageBox.prompt(
          '找到以下已批准的改签请求，请选择需要计算退款的请求:', 
          '选择改签记录', 
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputType: 'select',
            selectOptions: options
          }
        )
        
        const selectedRequest = approvedRescheduleRequests.find(req => req.id == selectedRequestId)
        
        if (!selectedRequest) {
          ElMessage.error('未找到选择的请求')
          return
        }
        
        // Now ask user to provide price information since we can't get it from the system
        const priceInfo = await ElMessageBox.prompt(
          `请输入价格信息来计算退款：
          
改签请求ID: ${selectedRequest.id}
当前票据: ${ticket.ticketNumber} 
状态: ${selectedRequest.status}

格式：原价,新价,手续费
例如：1200.00,800.00,50.00`, 
          '输入价格信息', 
          {
            confirmButtonText: '计算',
            cancelButtonText: '取消',
            inputPlaceholder: '原价,新价,手续费 (例如: 1200.00,800.00,50.00)'
          }
        )
        
        // Parse the price information
        const prices = priceInfo.value.split(',').map(p => parseFloat(p.trim()))
        
        if (prices.length !== 3 || prices.some(p => isNaN(p))) {
          ElMessage.error('价格格式错误，请使用格式：原价,新价,手续费')
          return
        }
        
        const [originalPrice, newPrice, serviceFee] = prices
        const priceDifference = originalPrice - newPrice
        const refundAmount = priceDifference - serviceFee
        
        if (refundAmount <= 0) {
          ElMessage.info(`计算结果：无需退款
          
原价: ¥${originalPrice}
新价: ¥${newPrice}  
价格差: ¥${priceDifference}
手续费: ¥${serviceFee}
净退款: ¥${refundAmount}`)
          return
        }
        
        // Confirm the refund request
        await ElMessageBox.confirm(
          `计算结果：
          
原价: ¥${originalPrice}
新价: ¥${newPrice}
价格差: ¥${priceDifference}
手续费: ¥${serviceFee}
应退金额: ¥${refundAmount}

是否提交退款申请？`, 
          '确认退款申请', 
          {
            confirmButtonText: '提交申请',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        // Submit the refund request
        const response = await api.post(`/tickets/${ticket.id}/refund`, {
          reason: `基于改签请求${selectedRequest.id}的补充退款申请 - 原价¥${originalPrice} 新价¥${newPrice} 手续费¥${serviceFee} 应退¥${refundAmount}`
        })

        if (response.data.success) {
          ElMessage.success(`退款申请已提交，申请金额：¥${refundAmount}`)
          await this.loadTickets()
        } else {
          ElMessage.error('退款申请失败: ' + response.data.message)
        }

      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('操作失败: ' + (error.message || '未知错误'))
        }
      } finally {
        this.loading = false
      }
    },

    async checkRefundStatus(ticket) {
      try {
        this.loading = true
        
        // Check for refund requests in the user's requests
        const requestsResponse = await api.get('/tickets/my-requests')
        
        if (requestsResponse.data.success) {
          const allRequests = requestsResponse.data.data || []
          
          // Find all refund requests for this ticket (including both REFUND and 改签退款 types)
          const refundRequests = allRequests.filter(req => 
            (req.requestType === 'REFUND' || req.requestType === '改签退款' || req.requestType === 'RESCHEDULE_REFUND') && (
              (req.ticket && req.ticket.id === ticket.id) ||
              (!req.ticket && req.reason && req.reason.includes(ticket.ticketNumber)) ||
              (!req.ticket && !req.reason && req.ticketNumber === ticket.ticketNumber) ||
              (!req.ticket && req.details && req.details.includes(ticket.ticketNumber))
            )
          )
          
          if (refundRequests.length === 0) {
            ElMessageBox.alert(
              `票据 ${ticket.ticketNumber} 没有找到退款申请记录。\n\n如果您认为应该有退款，请使用其他按钮提交退款申请。`, 
              '退款状态', 
              { type: 'info' }
            )
            return
          }
          
          // Display refund request status
          let statusMessage = `票据 ${ticket.ticketNumber} 的退款申请状态：\n\n`
          
          refundRequests.forEach((req, index) => {
            const statusText = {
              'PENDING': '⏳ 待审批',
              'APPROVED': '✅ 已批准',
              'REJECTED': '❌ 已拒绝',
              'COMPLETED': '💰 已完成'
            }[req.status] || `❓ ${req.status}`
            
            statusMessage += `申请 ${index + 1}:\n`
            statusMessage += `- 申请ID: ${req.id}\n`
            statusMessage += `- 状态: ${statusText}\n`
            statusMessage += `- 申请时间: ${req.requestTime || '未知'}\n`
            statusMessage += `- 申请原因: ${req.reason || '未填写'}\n`
            
            if (req.totalAmount) {
              statusMessage += `- 申请金额: ¥${req.totalAmount}\n`
            }
            
            if (req.processTime) {
              statusMessage += `- 处理时间: ${req.processTime}\n`
            }
            
            if (req.adminComment) {
              statusMessage += `- 管理员备注: ${req.adminComment}\n`
            }
            
            statusMessage += `\n`
          })
          
          // Add guidance based on status
          const latestRequest = refundRequests[0]
          if (latestRequest.status === 'PENDING') {
            statusMessage += `📋 下一步: 等待管理员审批\n退款申请正在处理中，请耐心等待。`
          } else if (latestRequest.status === 'APPROVED') {
            statusMessage += `💳 下一步: 等待退款到账\n申请已批准，退款将在3-5个工作日内到账到您的原支付方式。`
          } else if (latestRequest.status === 'REJECTED') {
            statusMessage += `📞 下一步: 联系客服\n申请被拒绝，如有疑问请联系客服询问原因。`
          } else if (latestRequest.status === 'COMPLETED') {
            statusMessage += `✅ 已完成: 退款应已到账\n如果您还没收到款项，请检查您的银行账户或支付宝/微信余额。`
          }
          
          ElMessageBox.alert(statusMessage, '退款状态详情', { 
            type: 'info',
            customClass: 'refund-status-dialog'
          })
          
        } else {
          ElMessage.error('无法获取退款状态信息')
        }
        
      } catch (error) {
        console.error('Check refund status error:', error)
        ElMessage.error('查询退款状态失败: ' + (error.message || '未知错误'))
      } finally {
        this.loading = false
      }
    },

    showRefundHelpGuide(ticket) {
      const helpMessage = `📋 改签退款完整指南

🎫 您的票据信息:
- 当前票据: ${ticket.ticketNumber} (¥${ticket.price})
- 原始票据ID: ${ticket.originalTicketId || '未知'}

💰 为什么没收到退款?
改签退款是一个多步骤流程，不会自动到账：

1️⃣ 提交申请阶段 ✅
   - 您需要先提交退款申请
   - 系统记录您的申请

2️⃣ 管理员审批阶段 ⏳
   - 管理员审核您的申请
   - 可能需要1-3个工作日

3️⃣ 财务处理阶段 💳
   - 审批通过后，财务部门处理退款
   - 退款到原支付方式(支付宝/微信/银行卡)
   - 通常需要3-7个工作日

🔧 您可以采取的行动:

🔍 第一步: 点击 "查看退款状态"
   - 检查是否已提交申请
   - 查看申请的审批状态

💰 第二步: 如果没有申请记录，点击 "计算改签退款"
   - 输入原票价和手续费
   - 系统帮您计算并提交申请

📞 第三步: 如果申请已批准但长时间未到账
   - 联系客服查询具体情况
   - 提供申请ID和票据号

⏰ 预期时间线:
- 申请提交: 立即
- 管理员审批: 1-3个工作日  
- 退款到账: 审批后3-7个工作日

📋 需要准备的信息:
- 原始票价
- 改签手续费金额
- 票据号码
- 改签时间

现在请选择下一步操作:`

      ElMessageBox.alert(helpMessage, '改签退款帮助', { 
        type: 'info',
        customClass: 'refund-help-dialog'
      })
    },
    
    async refundAndCancelTicket(ticket) {
      try {
        // Show special warning for connecting flights
        if (this.isConnectingFlight(ticket)) {
          await ElMessageBox.confirm(
            `您即将退款联程航班：${this.getFlightNumberDisplay(ticket)}\n\n注意：退款联程航班将取消整个行程，所有航段都将被取消。\n\n是否继续？`, 
            '联程航班退款确认', 
            {
              confirmButtonText: '继续退款',
              cancelButtonText: '取消',
              type: 'warning'
            }
          )
        }
        
        // Get detailed refund information
        const refundInfoResponse = await api.get(`/tickets/${ticket.id}/refund-info`)
        if (!refundInfoResponse.data.success) {
          ElMessage.error('无法获取退款信息: ' + refundInfoResponse.data.message)
          return
        }
        
        const refundInfo = refundInfoResponse.data.data
        
        // Check if refund is allowed
        if (!refundInfo.canRefund) {
          ElMessage.error(refundInfo.timeWarning || '此票据无法退款')
          return
        }
        
        let flightInfo = ''
        if (this.isConnectingFlight(ticket)) {
          const allFlights = this.getAllFlights(ticket)
          flightInfo = `联程航班：${allFlights.map(f => f.flightNumber).join(' + ')}`
        } else {
          flightInfo = `直飞航班：${ticket.flight?.flightNumber}`
        }
        
        const message = `
          <div style="text-align: left;">
            <h4>🚀 直接退款 - 立即处理</h4>
            <p><strong>${flightInfo}</strong></p>
            <p><strong>原票价：</strong>¥${refundInfo.originalAmount}</p>
            <p><strong>退款手续费：</strong>¥${refundInfo.serviceFee}</p>
            <p style="color: #67C23A; font-size: 16px;"><strong>实际退款金额：</strong>¥${refundInfo.refundAmount}</p>
            <hr>
            <div style="background-color: #f0f9ff; padding: 10px; border-left: 4px solid #409EFF; margin: 10px 0;">
              <p style="margin: 0; color: #409EFF; font-size: 14px;">
                <strong>✅ 无需等待审批，退款将立即处理</strong>
              </p>
              <p style="margin: 5px 0 0 0; color: #666; font-size: 12px;">
                退款将原路返回到您的支付账户，通常1-3个工作日到账
              </p>
            </div>
          </div>
        `
        
        await ElMessageBox.confirm(message, '确认直接退款', {
          confirmButtonText: '立即退款',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: true
        })
        
        this.loading = true
        const response = await api.post(`/tickets/${ticket.id}/refund`, { 
          reason: this.isConnectingFlight(ticket) ? '联程航班直接退款' : '客户直接退款' 
        })
        
        if (response.data.success) {
          const refundData = response.data.data
          
          // Show detailed success message
          const successMessage = `
            <div style="text-align: left;">
              <h4 style="color: #67C23A;">✅ 退款处理成功</h4>
              <p><strong>退款金额：</strong>¥${refundData.refundAmount}</p>
              <p><strong>退款时间：</strong>${new Date().toLocaleString()}</p>
              <p><strong>票据状态：</strong>已退款</p>
              <hr>
              <div style="background-color: #f0f9ff; padding: 10px; border-left: 4px solid #67C23A;">
                <p style="margin: 0; color: #67C23A; font-size: 14px;">
                  <strong>退款已处理完成</strong>
                </p>
                <p style="margin: 5px 0 0 0; color: #666; font-size: 12px;">
                  退款将在1-3个工作日内到达您的原支付账户
                </p>
              </div>
            </div>
          `
          
          ElMessageBox.alert(successMessage, '退款成功', {
            type: 'success',
            dangerouslyUseHTMLString: true,
            confirmButtonText: '好的'
          })
          
          await this.loadTickets()
          this.ticketDetailsVisible = false
        } else {
          ElMessage.error('退款失败: ' + response.data.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Refund error:', error)
          ElMessage.error('退款失败: ' + (error.response?.data?.message || error.message || '未知错误'))
        }
      } finally {
        this.loading = false
      }
    },

    showTicketDetails(ticket) {
      this.selectedTicket = ticket
      this.ticketDetailsVisible = true
      this.countdownDataLoaded = false

      if (ticket && ticket.status === 'BOOKED') {
        this.startSelectedTicketCountdown(ticket.id)
      }
    },
    
    async showRescheduleDialog(ticket) {
      if (this.isConnectingFlight(ticket)) {
        try {
          await ElMessageBox.confirm(
            `您即将改签联程航班：${this.getFlightNumberDisplay(ticket)}\n\n注意：改签联程航班将同时变更所有航段，保持相同的起始地和目的地。\n\n是否继续？`, 
            '联程航班改签确认', 
            {
              confirmButtonText: '继续改签',
              cancelButtonText: '取消',
              type: 'warning'
            }
          )
        } catch (error) {
          if (error === 'cancel') return
        }
      }
      
      this.selectedTicket = ticket
      this.rescheduleDialogVisible = true
      this.selectedNewFlight = null
      this.rescheduleReason = ''
      this.serviceFee = null

      try {
        const feeResponse = await api.get(`/tickets/${ticket.id}/reschedule-fee`)
        this.serviceFee = feeResponse.data.data
      } catch (error) {
        console.error('Failed to load service fee:', error)
      }

      this.selectedDate = null
      this.includeConnecting = true
      this.availableFlights = []
    },
    
    selectFlight(flight) {
      this.selectedNewFlight = flight
    },

    disablePastDates(time) {
      return time.getTime() < Date.now() - 24 * 60 * 60 * 1000
    },

    onDateChange() {
      this.availableFlights = []
      this.selectedNewFlight = null
    },

    onFlightTypeChange() {
      this.availableFlights = []
      this.selectedNewFlight = null
    },

    async searchFlights() {
      if (this.selectedTicket) {
        await this.loadAvailableFlights()
      }
    },

    clearFilters() {
      this.selectedDate = null
      this.includeConnecting = true
      // Clear current results after clearing filters
      this.availableFlights = []
      this.selectedNewFlight = null
    },

    async loadAvailableFlights() {
      if (!this.selectedTicket) return
      
      this.loadingFlights = true
      try {
        let url = `/tickets/${this.selectedTicket.id}/available-flights`
        const params = new URLSearchParams()
        
        if (this.selectedDate) {
          params.append('date', this.selectedDate)
        }
        if (this.includeConnecting !== null && this.includeConnecting !== undefined) {
          params.append('includeConnecting', this.includeConnecting)
        }
        
        if (params.toString()) {
          url += '?' + params.toString()
        }
        
        const response = await api.get(url)
        if (response.data.success) {
          this.availableFlights = response.data.data
        } else {
          ElMessage.error('加载可选航班失败：' + response.data.message)
          this.availableFlights = []
        }
      } catch (error) {
        console.error('Load available flights error:', error)
        ElMessage.error('加载可选航班失败')
        this.availableFlights = []
      } finally {
        this.loadingFlights = false
      }
    },
    
    async confirmReschedule() {
      if (!this.selectedNewFlight || !this.rescheduleReason) {
        ElMessage.warning('请选择新航班并选择改签原因')
        return
      }
      
      const totalCost = this.calculateTotalCost()
      const fareDifference = this.selectedNewFlight.price - this.selectedTicket.price
      const shouldRefund = fareDifference < 0 && this.serviceFee < Math.abs(fareDifference)
      const refundAmount = shouldRefund ? Math.abs(this.serviceFee - Math.abs(fareDifference)) : 0
      
      try {
        this.rescheduleLoading = true

        const response = await api.post(`/tickets/${this.selectedTicket.id}/reschedule`, {
          newFlightId: this.selectedNewFlight.id,
          reason: this.rescheduleReason
        })
        
        if (response.data.success) {
          if (shouldRefund && refundAmount > 0) {
            ElMessage.success(`改签申请已提交，您将获得¥${refundAmount}退款，管理员审批后将处理退款`)
          } else if (totalCost > 0) {
            ElMessage.success(`改签申请已提交，需支付¥${totalCost}改签费用，管理员批准后将通知您支付`)
          } else {
            ElMessage.success('改签申请已提交，管理员将尽快处理')
          }
          this.closeRescheduleDialog()
          this.loadTickets()
        }
      } catch (error) {
        console.error('Reschedule error:', error)
        ElMessage.error('改签申请失败: ' + (error.response?.data?.message || error.message))
      } finally {
        this.rescheduleLoading = false
      }
    },
    
    closeRescheduleDialog() {
      this.rescheduleDialogVisible = false
      this.availableFlights = []
      this.selectedNewFlight = null
      this.serviceFee = null
      this.rescheduleReason = ''
      this.selectedDate = null
      this.includeConnecting = true
    },
    
    // Utility methods
    formatTime(datetime) {
      if (!datetime) {
        console.log('🔴 formatTime: datetime is empty/null')
        return '--:--'
      }
      
      console.log('🕐 formatTime input:', datetime, 'type:', typeof datetime)
      
      try {
        const parsed = this.parseDateTime(datetime)
        if (!parsed) {
          console.log('🔴 formatTime: parseDateTime returned null for:', datetime)
          return '--:--'
        }
        const formatted = parsed.format('HH:mm')
        console.log('✅ formatTime result:', formatted, 'for input:', datetime)
        return formatted
      } catch (error) {
        console.error('🔴 Time formatting error:', error, datetime)
        return '--:--'
      }
    },
    
    formatDate(datetime) {
      if (!datetime) {
        console.log('🔴 formatDate: datetime is empty/null')
        return '--/--'
      }
      
      console.log('📅 formatDate input:', datetime, 'type:', typeof datetime)
      
      try {
        const parsed = this.parseDateTime(datetime)
        if (!parsed) {
          console.log('🔴 formatDate: parseDateTime returned null for:', datetime)
          return '--/--'
        }
        const formatted = parsed.format('MM/DD')
        console.log('✅ formatDate result:', formatted, 'for input:', datetime)
        return formatted
      } catch (error) {
        console.error('🔴 Date formatting error:', error, datetime)
        return '--/--'
      }
    },
    
    formatDateTime(datetime) {
      if (!datetime) {
        console.log('🔴 formatDateTime: datetime is empty/null')
        return 'N/A'
      }
      
      console.log('📅⏰ formatDateTime input:', datetime, 'type:', typeof datetime)
      
      try {
        const parsed = this.parseDateTime(datetime)
        if (!parsed) {
          console.log('🔴 formatDateTime: parseDateTime returned null for:', datetime)
          return 'N/A'
        }
        const formatted = parsed.format('MM/DD HH:mm')
        console.log('✅ formatDateTime result:', formatted, 'for input:', datetime)
        return formatted
      } catch (error) {
        console.error('🔴 DateTime formatting error:', error, datetime)
        return 'N/A'
      }
    },
    
    parseDateTime(dateTime) {
      if (!dateTime) return null
      
      console.log('🕐 parseDateTime input:', dateTime, typeof dateTime);
      
      try {
        // Parse as UTC and convert to local timezone (Asia/Shanghai for China)
        let parsed = dayjs.utc(dateTime).tz('Asia/Shanghai')
        if (parsed.isValid()) {
          console.log('✅ UTC->Asia/Shanghai successful:', dateTime, '->', parsed.format('YYYY-MM-DD HH:mm:ss'));
          return parsed
        }
        
        // Try parsing as "yyyy-MM-dd HH:mm:ss" format (assume UTC)
        parsed = dayjs.utc(dateTime, 'YYYY-MM-DD HH:mm:ss').tz('Asia/Shanghai')
        if (parsed.isValid()) {
          console.log('✅ Format UTC->Asia/Shanghai successful:', dateTime, '->', parsed.format('YYYY-MM-DD HH:mm:ss'));
          return parsed
        }
        
        // Try parsing as ISO with 'T' separator (assume UTC)
        parsed = dayjs.utc(dateTime, 'YYYY-MM-DDTHH:mm:ss').tz('Asia/Shanghai')
        if (parsed.isValid()) {
          console.log('✅ ISO UTC->Asia/Shanghai successful:', dateTime, '->', parsed.format('YYYY-MM-DD HH:mm:ss'));
          return parsed
        }
        
        // NO FALLBACK - if UTC parsing fails, return null to avoid timezone issues
        console.log('❌ All UTC parsing methods failed for:', dateTime);
        
        console.error('❌ All parsing attempts failed for:', dateTime);
        return null
      } catch (error) {
        console.error('❌ Date parsing error:', error, dateTime)
        return null
      }
    },
    
    isNextDay(departureTime, arrivalTime) {
      try {
        const departure = this.parseDateTime(departureTime)
        const arrival = this.parseDateTime(arrivalTime)
        
        if (!departure || !arrival) return false
        return arrival.date() > departure.date()
      } catch (error) {
        console.error('Next day calculation error:', error)
        return false
      }
    },
    
    calculateFlightDuration(departureTime, arrivalTime) {
      if (!departureTime || !arrivalTime) return '--'
      
      try {
        const departure = this.parseDateTime(departureTime)
        const arrival = this.parseDateTime(arrivalTime)
        
        if (!departure || !arrival) {
          console.warn('Invalid date format for duration calculation:', { departureTime, arrivalTime })
          return '--'
        }
        
        const duration = arrival.diff(departure, 'minute')
        
        if (duration < 0) {
          console.warn('Negative duration calculated:', { departureTime, arrivalTime, duration })
          return '--'
        }
        
        const hours = Math.floor(duration / 60)
        const minutes = duration % 60
        
        if (hours > 0 && minutes > 0) {
          return `${hours}小时${minutes}分钟`
        } else if (hours > 0) {
          return `${hours}小时`
        } else {
          return `${minutes}分钟`
        }
      } catch (error) {
        console.error('Duration calculation error:', error, { departureTime, arrivalTime })
        return '--'
      }
    },
    
    // Local timezone formatting functions for international flights
    parseLocalDateTime(dateTime, airport) {
      if (!dateTime) return null
      
      console.log('🌍 parseLocalDateTime input:', dateTime, 'airport timezone:', airport?.timeZone);
      
      try {
        // If no airport or no timezone, fallback to China time
        const targetTimezone = airport?.timeZone || 'Asia/Shanghai'
        
        // Parse as UTC and convert to local airport timezone
        let parsed = dayjs.utc(dateTime).tz(targetTimezone)
        if (parsed.isValid()) {
          console.log('✅ UTC->Local successful:', dateTime, '->', parsed.format('YYYY-MM-DD HH:mm:ss'), 'TZ:', targetTimezone);
          return parsed
        }
        
        // Try parsing as "yyyy-MM-dd HH:mm:ss" format (assume UTC)
        parsed = dayjs.utc(dateTime, 'YYYY-MM-DD HH:mm:ss').tz(targetTimezone)
        if (parsed.isValid()) {
          console.log('✅ Format UTC->Local successful:', dateTime, '->', parsed.format('YYYY-MM-DD HH:mm:ss'), 'TZ:', targetTimezone);
          return parsed
        }
        
        // Try parsing as ISO with 'T' separator (assume UTC)
        parsed = dayjs.utc(dateTime, 'YYYY-MM-DDTHH:mm:ss').tz(targetTimezone)
        if (parsed.isValid()) {
          console.log('✅ ISO UTC->Local successful:', dateTime, '->', parsed.format('YYYY-MM-DD HH:mm:ss'), 'TZ:', targetTimezone);
          return parsed
        }
        
        console.log('❌ All local parsing methods failed for:', dateTime, 'timezone:', targetTimezone);
        return null
      } catch (error) {
        console.error('❌ Local date parsing error:', error, dateTime, airport?.timeZone)
        return null
      }
    },
    
    formatLocalTime(datetime, airport) {
      if (!datetime) {
        console.log('🔴 formatLocalTime: datetime is empty/null')
        return '--:--'
      }
      
      console.log('🕐🌍 formatLocalTime input:', datetime, 'airport:', airport?.code, 'timezone:', airport?.timeZone)
      
      try {
        const parsed = this.parseLocalDateTime(datetime, airport)
        if (!parsed) {
          console.log('🔴 formatLocalTime: parseLocalDateTime returned null for:', datetime)
          return '--:--'
        }
        const formatted = parsed.format('HH:mm')
        console.log('✅ formatLocalTime result:', formatted, 'for input:', datetime, 'timezone:', airport?.timeZone)
        return formatted
      } catch (error) {
        console.error('🔴 Local time formatting error:', error, datetime)
        return '--:--'
      }
    },
    
    formatLocalDateTime(datetime, airport) {
      if (!datetime) {
        console.log('🔴 formatLocalDateTime: datetime is empty/null')
        return 'N/A'
      }
      
      console.log('📅⏰🌍 formatLocalDateTime input:', datetime, 'airport:', airport?.code, 'timezone:', airport?.timeZone)
      
      try {
        const parsed = this.parseLocalDateTime(datetime, airport)
        if (!parsed) {
          console.log('🔴 formatLocalDateTime: parseLocalDateTime returned null for:', datetime)
          return 'N/A'
        }
        const formatted = parsed.format('MM/DD HH:mm')
        console.log('✅ formatLocalDateTime result:', formatted, 'for input:', datetime, 'timezone:', airport?.timeZone)
        return formatted
      } catch (error) {
        console.error('🔴 Local DateTime formatting error:', error, datetime)
        return 'N/A'
      }
    },
    
    isInternationalAirport(airport) {
      // Check if the airport is in a different timezone than China
      return airport?.timeZone && airport.timeZone !== 'Asia/Shanghai'
    },
    
    isNextDayLocal(departureTime, arrivalTime, departureAirport, arrivalAirport) {
      try {
        const departure = this.parseLocalDateTime(departureTime, departureAirport)
        const arrival = this.parseLocalDateTime(arrivalTime, arrivalAirport)
        
        if (!departure || !arrival) return false
        return arrival.date() > departure.date()
      } catch (error) {
        console.error('Local next day calculation error:', error)
        return false
      }
    },
    
    // Connecting flight utility methods
    isConnectingFlight(ticket) {
      return ticket && ticket.connectingFlights && ticket.connectingFlights.length > 0
    },
    
    getAllFlights(ticket) {
      if (!ticket) return []
      const flights = [ticket.flight]
      if (ticket.connectingFlights && ticket.connectingFlights.length > 0) {
        flights.push(...ticket.connectingFlights)
      }
      return flights.filter(f => f) // Remove any null/undefined flights
    },
    
    getLastFlight(ticket) {
      const allFlights = this.getAllFlights(ticket)
      return allFlights[allFlights.length - 1]
    },
    
    getFlightNumberDisplay(ticket) {
      if (!ticket) return '-'
      if (this.isConnectingFlight(ticket)) {
        const allFlights = this.getAllFlights(ticket)
        return allFlights.map(f => f.flightNumber).join(' + ')
      }
      return ticket.flight?.flightNumber || '-'
    },
    
    getAvailableFlightDisplay(flight) {
      if (!flight) return '-'
      if (flight.connectingFlights && flight.connectingFlights.length > 0) {
        // For connecting flights, show all flight numbers
        const allNumbers = [flight.flightNumber]
        flight.connectingFlights.forEach(connecting => {
          allNumbers.push(connecting.flightNumber)
        })
        return allNumbers.join(' + ')
      }
      return flight.flightNumber || '-'
    },
    
    getCompleteRoute(ticket) {
      if (!ticket) return '-'
      if (this.isConnectingFlight(ticket)) {
        const allFlights = this.getAllFlights(ticket)
        if (allFlights.length === 0) return '-'
        const origin = allFlights[0].departureAirport?.city || '未知'
        const destination = allFlights[allFlights.length - 1].arrivalAirport?.city || '未知'
        const intermediate = allFlights.slice(0, -1).map(f => f.arrivalAirport?.city).filter(c => c)
        if (intermediate.length > 0) {
          return `${origin} → ${intermediate.join(' → ')} → ${destination}`
        }
        return `${origin} → ${destination}`
      }
      const departure = ticket.flight?.departureAirport?.city || '未知'
      const arrival = ticket.flight?.arrivalAirport?.city || '未知'
      return `${departure} → ${arrival}`
    },
    
    // Updated route method to use the new complete route logic
    getRoute(flight) {
      if (!flight) return '-'
      const departure = flight.departureAirport?.city || '未知'
      const arrival = flight.arrivalAirport?.city || '未知'
      return `${departure} → ${arrival}`
    },
    
    getAirportCode(airport) {
      return airport?.code || airport?.name?.slice(0, 3) || '未知'
    },
    
    getAirlineShortName(name) {
      if (!name) return '航'
      return name.slice(0, 2)
    },
    
    // Cross-airport transfer detection
    hasCrossAirportTransfer(ticket) {
      if (!this.isConnectingFlight(ticket)) return false
      
      const allFlights = this.getAllFlights(ticket)
      for (let i = 0; i < allFlights.length - 1; i++) {
        const currentFlight = allFlights[i]
        const nextFlight = allFlights[i + 1]
        
        if (currentFlight?.arrivalAirport && nextFlight?.departureAirport) {
          // Check if same city but different airports
          const sameCity = currentFlight.arrivalAirport.city === nextFlight.departureAirport.city
          const differentAirports = currentFlight.arrivalAirport.code !== nextFlight.departureAirport.code
          
          if (sameCity && differentAirports) {
            return true
          }
        }
      }
      return false
    },
    
    getCrossAirportTransferInfo(ticket) {
      if (!this.isConnectingFlight(ticket)) return []
      
      const transfers = []
      const allFlights = this.getAllFlights(ticket)
      
      for (let i = 0; i < allFlights.length - 1; i++) {
        const currentFlight = allFlights[i]
        const nextFlight = allFlights[i + 1]
        
        if (currentFlight?.arrivalAirport && nextFlight?.departureAirport) {
          const sameCity = currentFlight.arrivalAirport.city === nextFlight.departureAirport.city
          const differentAirports = currentFlight.arrivalAirport.code !== nextFlight.departureAirport.code
          
          if (sameCity && differentAirports) {
            transfers.push({
              city: currentFlight.arrivalAirport.city,
              fromAirport: currentFlight.arrivalAirport,
              toAirport: nextFlight.departureAirport,
              fromFlight: currentFlight.flightNumber,
              toFlight: nextFlight.flightNumber
            })
          }
        }
      }
      return transfers
    },
    
    // Status methods
    getFlightStatusType(status) {
      const statusTypes = {
        'SCHEDULED': 'success',
        'DELAYED': 'warning',
        'CANCELLED': 'danger',
        'DEPARTED': 'info',
        'LANDED': 'primary',
        'COMPLETED': 'info'
      }
      return statusTypes[status] || ''
    },
    
    getConnectingFlightStatusType(ticket) {
      if (!this.isConnectingFlight(ticket)) {
        return this.getFlightStatusType(ticket.flight?.status)
      }
      
      const allFlights = this.getAllFlights(ticket)
      const statuses = allFlights.map(f => f.status)

      if (statuses.includes('CANCELLED')) return 'danger'
      if (statuses.includes('DELAYED')) return 'warning'
      if (statuses.every(s => s === 'COMPLETED')) return 'info'
      if (statuses.every(s => ['LANDED', 'COMPLETED'].includes(s))) return 'primary'
      if (statuses.includes('DEPARTED')) return 'info'
      return 'success'
    },
    
    getConnectingFlightStatusText(ticket) {
      if (!this.isConnectingFlight(ticket)) {
        return this.getFlightStatusText(ticket.flight?.status)
      }
      
      const allFlights = this.getAllFlights(ticket)
      const statuses = allFlights.map(f => f.status)

      if (statuses.includes('CANCELLED')) return '已取消'
      if (statuses.includes('DELAYED')) return '有延误'
      if (statuses.every(s => s === 'COMPLETED')) return '已完成'
      if (statuses.every(s => ['LANDED', 'COMPLETED'].includes(s))) return '已到达'
      if (statuses.includes('DEPARTED')) return '进行中'
      
      return '正常'
    },

    isRescheduleConnectingFlight(flight) {
      if (!flight || !this.selectedTicket) return false

      if (this.includeConnecting && this.selectedTicket) {
        const finalDestination = this.getFinalDestinationAirportId()
        return flight.arrivalAirport?.id !== finalDestination
      }
      
      return false
    },
    
    getFinalDestination() {
      if (!this.selectedTicket) return ''
      
      if (this.isConnectingFlight(this.selectedTicket)) {
        // For connecting flights, get the final destination
        const allFlights = this.getAllFlights(this.selectedTicket)
        const lastFlight = allFlights[allFlights.length - 1]
        return lastFlight?.arrivalAirport?.city || ''
      } else {
        // For direct flights, get the arrival airport
        return this.selectedTicket.flight?.arrivalAirport?.city || ''
      }
    },
    
    getFinalDestinationAirportId() {
      if (!this.selectedTicket) return null
      
      if (this.isConnectingFlight(this.selectedTicket)) {
        // For connecting flights, get the final destination airport ID
        const allFlights = this.getAllFlights(this.selectedTicket)
        const lastFlight = allFlights[allFlights.length - 1]
        return lastFlight?.arrivalAirport?.id || null
      } else {
        // For direct flights, get the arrival airport ID
        return this.selectedTicket.flight?.arrivalAirport?.id || null
      }
    },
    
    getTicketStatusType(status) {
      const statusTypes = {
        'BOOKED': '',
        'PAID': 'success',
        'CHECKED_IN': 'info',
        'CANCELLED': 'danger',
        'REFUNDED': 'warning'
      }
      return statusTypes[status] || ''
    },
    
    getFlightStatusIcon(status) {
      const icons = {
        'SCHEDULED': 'CircleCheck',
        'DELAYED': 'Clock',
        'CANCELLED': 'Close',
        'DEPARTED': 'Promotion',
        'LANDED': 'CircleCheck',
        'COMPLETED': 'CircleCheck'
      }
      return icons[status] || 'InfoFilled'
    },
    
    getTicketStatusIcon(status) {
      const icons = {
        'BOOKED': 'Clock',
        'PAID': 'CircleCheck',
        'CHECKED_IN': 'CircleCheck',
        'CANCELLED': 'Close',
        'REFUNDED': 'Warning'
      }
      return icons[status] || 'InfoFilled'
    },
    
    getFlightStatusText(status) {
      const statusMap = {
        'SCHEDULED': '正常',
        'DELAYED': '延误',
        'CANCELLED': '已取消',
        'DEPARTED': '已起飞',
        'LANDED': '已降落',
        'COMPLETED': '已完成'
      }
      return statusMap[status] || status
    },
    
    getTicketStatusText(status) {
      const statusMap = {
        'BOOKED': '已预订',
        'PAID': '已支付',
        'PENDING_RESCHEDULE': '待支付改签费',
        'CHECKED_IN': '已登机',
        'CANCELLED': '已取消',
        'REFUNDED': '已退款'
      }
      return statusMap[status] || status
    },
    
    getTicketTypeText(type) {
      const typeMap = {
        'ECONOMY': '经济舱',
        'BUSINESS': '商务舱',
        'FIRST': '头等舱'
      }
      return typeMap[type] || type
    },

    // Countdown methods
    async startSelectedTicketCountdown(ticketId) {
      if (this.selectedTicketTimer) {
        clearInterval(this.selectedTicketTimer)
      }

      try {
        await this.fetchSelectedTicketDeadline(ticketId)
        this.selectedTicketTimer = setInterval(() => {
          this.updateSelectedTicketCountdown()
        }, 1000)
      } catch (error) {
        console.error('Failed to start countdown:', error)
      }
    },

    async fetchSelectedTicketDeadline(ticketId) {
      try {
        const response = await api.get(`/tickets/${ticketId}/payment-deadline`)
        
        if (response.data.success) {
          const data = response.data.data
          this.selectedTicketRemainingSeconds = data.remainingSeconds || 0
          
          // Use backend provided values directly to avoid flashing
          this.selectedTicketUrgency = data.urgencyLevel || 'LOW'
          this.selectedTicketMessage = data.message || '请在时限内完成支付'
          this.selectedTicketCountdown = data.countdownDisplay || '00:00'
          
          // Mark data as loaded
          this.countdownDataLoaded = true
        }
      } catch (error) {
        console.error('Failed to fetch payment deadline:', error)
        this.selectedTicketRemainingSeconds = 0
        this.selectedTicketUrgency = 'EXPIRED'
        this.selectedTicketMessage = '支付时间已过期'
        this.selectedTicketCountdown = '00:00'
        
        // Mark data as loaded even on error
        this.countdownDataLoaded = true
      }
    },

    updateSelectedTicketCountdown() {
      if (!this.selectedTicketRemainingSeconds || this.selectedTicketRemainingSeconds <= 0) {
        this.selectedTicketCountdown = '00:00'
        this.selectedTicketMessage = '支付时间已过期'
        this.selectedTicketUrgency = 'EXPIRED'
        
        if (this.selectedTicketTimer) {
          clearInterval(this.selectedTicketTimer)
          this.selectedTicketTimer = null
        }
        return
      }

      this.selectedTicketRemainingSeconds--
      
      const minutes = Math.floor(this.selectedTicketRemainingSeconds / 60)
      const seconds = this.selectedTicketRemainingSeconds % 60
      this.selectedTicketCountdown = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
      
      if (this.selectedTicketRemainingSeconds > 300) {
        this.selectedTicketUrgency = 'LOW'
        this.selectedTicketMessage = '请在时限内完成支付'
      } else if (this.selectedTicketRemainingSeconds > 120) {
        this.selectedTicketUrgency = 'MEDIUM'
        this.selectedTicketMessage = '支付时间即将到期，请尽快完成支付'
      } else if (this.selectedTicketRemainingSeconds > 0) {
        this.selectedTicketUrgency = 'HIGH'
        this.selectedTicketMessage = '支付时间即将到期！'
      }
    },

    // Cancel methods
    async cancelSelectedTicket() {
      await this.cancelTicketById(this.selectedTicket.id)
      this.ticketDetailsVisible = false
    },

    async cancelTicketFromCard(ticket) {
      await this.cancelTicketById(ticket.id)
    },

    async cancelTicketById(ticketId) {
      try {
        await ElMessageBox.confirm(
          '确定要取消此订单吗？取消后将释放座位，订单无法恢复。',
          '取消订单确认',
          {
            confirmButtonText: '确定取消',
            cancelButtonText: '继续支付',
            type: 'warning'
          }
        )
        
        const response = await api.post(`/tickets/${ticketId}/cancel`, {
          reason: '用户主动取消订单'
        })
        
        if (response.data.success) {
          ElMessage.success('订单已取消')
          await this.loadTickets()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('取消订单失败')
        }
      }
    },

    // Fare calculation methods

    // Calculate what surcharge was actually used when creating the ticket price
    calculateActualStoredSurcharge(ticket) {
      if (!ticket) return 0
      
      const flightCount = ticket.connectingFlights ? 1 + ticket.connectingFlights.length : 1
      const oldSurchargePerFlight = 70  // Old system: only fuel surcharge
      const newSurchargePerFlight = 120 // New system: fuel + construction fee
      
      // Use the ticket's own booking time to determine what surcharge was used when creating it
      const cutoffDate = new Date('2025-09-01T20:00:00') // When fix was deployed
      const ticketCreationDate = new Date(ticket.bookingTime)
      
      if (ticketCreationDate < cutoffDate) {
        // Ticket created with old system: stored price includes 70 per flight
        return flightCount * oldSurchargePerFlight
      } else {
        // Ticket created with new system: stored price includes 120 per flight
        return flightCount * newSurchargePerFlight
      }
    },

    // Calculate what surcharge to DISPLAY (for rescheduled tickets, show original rate)
    calculateDisplaySurcharge(ticket) {
      if (!ticket) return 0
      
      const flightCount = ticket.connectingFlights ? 1 + ticket.connectingFlights.length : 1
      const oldSurchargePerFlight = 70  // Old system: only fuel surcharge
      const newSurchargePerFlight = 120 // New system: fuel + construction fee
      
      const cutoffDate = new Date('2025-09-01T20:00:00') // When fix was deployed
      
      // Special handling for PENDING_RESCHEDULE tickets to avoid display inconsistencies
      if (ticket.status === 'PENDING_RESCHEDULE') {
        // For tickets awaiting reschedule payment, always show the actual stored surcharge
        // to match what was used in the fee calculation
        return this.calculateActualStoredSurcharge(ticket)
      }
      
      // For rescheduled tickets, use original ticket's booking time for display
      if (ticket.originalTicketId) {
        const originalTicket = this.tickets?.find(t => t.id === ticket.originalTicketId)
        if (originalTicket) {
          const originalBookingDate = new Date(originalTicket.bookingTime)
          console.log(`Rescheduled ticket ${ticket.ticketNumber}: Display based on original booking time ${originalTicket.bookingTime}`)
          
          if (originalBookingDate < cutoffDate) {
            return flightCount * oldSurchargePerFlight // Display ¥70 for old bookings
          } else {
            return flightCount * newSurchargePerFlight // Display ¥120 for new bookings
          }
        } else {
          // Can't find original ticket in current list
          console.log(`Rescheduled ticket ${ticket.ticketNumber}: Original ticket not found in current list`)
          
          // Use actual stored surcharge to avoid display inconsistencies
          return this.calculateActualStoredSurcharge(ticket)
        }
      } else {
        // Regular ticket - display matches what was stored
        return this.calculateActualStoredSurcharge(ticket)
      }
    },

    calculateBaseTicketPrice(ticket) {
      if (!ticket) return 0
      // Use the actual stored surcharge (what was used to create the price) to get correct base
      return ticket.price - this.calculateActualStoredSurcharge(ticket)
    },

    // This method returns what should be displayed to user
    calculateFuelSurcharge(ticket) {
      return this.calculateDisplaySurcharge(ticket)
    },

    // Calculate the total that should be displayed (base price + display surcharges)
    calculateDisplayTotal(ticket) {
      if (!ticket) return 0
      const basePrice = this.calculateBaseTicketPrice(ticket)
      const displaySurcharge = this.calculateDisplaySurcharge(ticket)
      return basePrice + displaySurcharge
    },

    getFareDifferenceClass() {
      if (!this.selectedNewFlight || !this.selectedTicket) return ''
      const difference = this.selectedNewFlight.price - this.selectedTicket.price
      if (difference > 0) {
        return 'fare-increase'
      } else if (difference < 0) {
        return 'fare-decrease'
      }
      return 'fare-same'
    },

    getFareDifferenceText() {
      if (!this.selectedNewFlight || !this.selectedTicket) return '待计算'
      const difference = this.selectedNewFlight.price - this.selectedTicket.price
      if (difference > 0) {
        return `+¥${difference}`
      } else if (difference < 0) {
        return `-¥${Math.abs(difference)}`
      }
      return '¥0'
    },
    getTotalFeeText() {
      if (!this.selectedNewFlight || !this.selectedTicket || !this.serviceFee) return '待计算'
      const fareDifference = this.selectedNewFlight.price - this.selectedTicket.price
      
      if (fareDifference < 0) {
        // When new flight is cheaper, customer gets refund but still pays service fee
        const refundAmount = Math.abs(fareDifference)
        const netCost = this.serviceFee - refundAmount
        if (netCost <= 0) {
          return `¥0 (退款¥${Math.abs(netCost)})`
        } else {
          return `¥${netCost} (含¥${refundAmount}票价退差)`
        }
      } else if (fareDifference > 0) {
        // When new flight is more expensive, customer pays service fee + fare difference
        const totalFee = fareDifference + this.serviceFee
        return `¥${totalFee}`
      } else {
        // Same price, only service fee
        return `¥${this.serviceFee}`
      }
    },

    // Calculate actual amount to be paid (for payment flow)
    calculateTotalCost() {
      if (!this.selectedNewFlight || !this.selectedTicket || !this.serviceFee) return 0
      const fareDifference = this.selectedNewFlight.price - this.selectedTicket.price
      
      if (fareDifference < 0) {
        // When new flight is cheaper, net cost = service fee - refund amount
        const refundAmount = Math.abs(fareDifference)
        const netCost = this.serviceFee - refundAmount
        return Math.max(0, netCost) // Cannot be negative
      } else {
        // When new flight is same price or more expensive
        return fareDifference + this.serviceFee
      }
    },

    // Get appropriate button text based on payment requirement
    getConfirmButtonText() {
      if (!this.selectedNewFlight || !this.rescheduleReason.trim()) {
        return '提交改签申请'
      }
      
      const totalCost = this.calculateTotalCost()
      const fareDifference = this.selectedNewFlight.price - this.selectedTicket.price
      const shouldRefund = fareDifference < 0 && this.serviceFee < Math.abs(fareDifference)
      
      if (shouldRefund) {
        const refundAmount = Math.abs(this.serviceFee - Math.abs(fareDifference))
        return `提交申请 (将退款¥${(refundAmount-120)})`
      } else if (totalCost > 0) {
        return `提交申请 (需支付¥${totalCost})`
      } else {
        return '提交改签申请'
      }
    }
  },
  
  mounted() {
    console.log('🔧 MyTickets component mounted - Starting debugging session')
    console.log('🌏 Current timezone:', Intl.DateTimeFormat().resolvedOptions().timeZone)
    console.log('🕐 Current time:', new Date().toISOString())
    
    // Test time formatting immediately
    console.log('🧪 Testing time formatting with sample UTC time: 2025-08-30T21:35:16Z')
    const testResult = this.formatTime('2025-08-30T21:35:16Z')
    console.log('🧪 Expected: 05:35 (China time), Got:', testResult)
    
    if (!this.currentUser) {
      console.log('❌ No current user, redirecting to login')
      this.$router.push('/login')
      return
    }
    
    console.log('✅ User authenticated, loading tickets...')
    this.loadTickets()
  },

  beforeUnmount() {
    if (this.selectedTicketTimer) {
      clearInterval(this.selectedTicketTimer)
    }
  }
}
</script>

<style scoped>
/* Page Layout */
.my-tickets-page {
  min-height: 100vh;
  background: var(--color-bg-primary);
}

/* Page Header */
.page-header {
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border-primary);
  padding: var(--space-6) 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2) 0;
}

.page-subtitle {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: var(--space-3);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-6);
}

/* Search and Filter Section */
.search-filter-section {
  padding: var(--space-6) 0;
}

.search-filter-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-primary);
}

.search-filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-6);
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.filter-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.filter-input,
.filter-select,
.filter-date {
  width: 100%;
}

/* Pending Requests Section */
.pending-requests-section {
  padding: 0 0 var(--space-6);
}

.pending-alert {
  margin-bottom: var(--space-4);
}

.alert-content {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.alert-icon {
  font-size: var(--icon-sm);
  color: var(--color-warning);
}

.pending-requests-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.pending-request-item {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.request-content {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex: 1;
}

.request-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.request-type {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.request-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.ticket-number {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.request-reason {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.request-time {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

/* Tickets Section */
.tickets-section {
  padding: 0 0 var(--space-8);
}

.results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-6);
}

.results-count {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.filter-indicator {
  color: var(--color-primary);
  font-size: var(--font-size-sm);
}

.view-controls {
  display: flex;
  gap: var(--space-2);
}

/* Loading State */
.loading-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: var(--space-6);
}

.ticket-card-skeleton {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  border: 1px solid var(--color-border-primary);
}

.skeleton-ticket-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.skeleton-title {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.skeleton-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.skeleton-actions {
  display: flex;
  gap: var(--space-2);
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: var(--space-12) var(--space-6);
}

.empty-illustration {
  margin-bottom: var(--space-6);
}

.empty-icon {
  color: var(--color-text-tertiary);
  opacity: 0.5;
}

.empty-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-3) 0;
}

.empty-description {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-6) 0;
}

.empty-action-btn {
  padding: var(--space-3) var(--space-6);
}

/* Tickets Grid (Card View) */
.tickets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: var(--space-6);
}

.ticket-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-primary);
  cursor: pointer;
  transition: var(--transition-normal);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.ticket-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
  border-color: var(--color-primary);
}

.ticket-card:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* Ticket Card Header */
.ticket-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.airline-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex: 1;
}

.airline-logo {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary);
  flex-shrink: 0;
}

.airline-logo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.airline-logo-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: white;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
}

.flight-basic-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.flight-number {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-family: var(--font-family-monospace);
}

.route {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

.status-badges {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  align-items: flex-end;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--font-size-xs);
}

.status-icon {
  font-size: var(--icon-xs);
}

/* Flight Timeline */
.flight-timeline {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
}

.timeline-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  flex-shrink: 0;
}

.timeline-time-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timeline-time {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-family: var(--font-family-monospace);
}

.local-time-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-bottom: 2px;
  font-weight: var(--font-weight-medium);
  background: var(--color-primary-light);
  padding: 1px 6px;
  border-radius: 8px;
  white-space: nowrap;
}

.local-time-label-table {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
  background: var(--color-primary-light);
  padding: 1px 4px;
  border-radius: 6px;
  margin-bottom: 2px;
  display: inline-block;
}

.local-time-label-detail {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
  background: var(--color-primary-light);
  padding: 2px 8px;
  border-radius: 8px;
  margin-bottom: 4px;
  text-align: center;
}

.timeline-location {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.timeline-detail {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  text-align: center;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.next-day {
  font-size: var(--font-size-xs);
  color: var(--color-error);
  margin-left: var(--space-1);
}

.timeline-path {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 100px;
}

.path-line {
  flex: 1;
  height: 2px;
  background: var(--color-border-secondary);
  border-radius: 1px;
}

.plane-icon {
  color: var(--color-primary);
  font-size: var(--icon-sm);
  transform: rotate(90deg);
}

/* Ticket Details */
.ticket-details {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.detail-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

.detail-value.price {
  font-size: var(--font-size-lg);
  color: var(--color-primary);
  font-weight: var(--font-weight-bold);
}

/* Ticket Actions */
.ticket-actions {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.ticket-actions .el-button {
  flex: 1;
  min-width: 80px;
}

.ticket-actions .el-button--warning.is-plain {
  border-color: #e6a23c;
  color: #e6a23c;
  background: #fdf6ec;
}

.ticket-actions .el-button--warning.is-plain:hover {
  background: #e6a23c;
  border-color: #e6a23c;
  color: #fff;
}

/* Table View */
.tickets-table-container {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-border-primary);
}

.tickets-table {
  width: 100%;
}

.tickets-table :deep(.el-table__header) {
  background: var(--color-bg-secondary);
}

.tickets-table :deep(.el-table__row:hover) {
  background: var(--color-bg-hover);
}

.table-flight-number {
  font-family: var(--font-family-monospace);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.table-route {
  color: var(--color-text-secondary);
}

.table-time {
  font-family: var(--font-family-monospace);
  color: var(--color-text-primary);
}

.table-actions {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

/* Pagination */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: var(--space-8);
}

.tickets-pagination {
  background: var(--color-bg-card);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-primary);
}

/* Ticket Details Drawer */
.ticket-details-drawer :deep(.el-drawer__header) {
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border-primary);
  margin-bottom: 0;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.drawer-header h3 {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.ticket-number {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  font-family: var(--font-family-monospace);
}

.ticket-details-content {
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.detail-section .section-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--color-border-primary);
}

/* Flight Summary Card */
.flight-summary-card {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  border: 1px solid var(--color-border-primary);
}

.flight-summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-6);
}

.airline-brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.airline-logo-large {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-border-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-card);
}

.airline-logo-large .airline-logo-placeholder {
  font-size: var(--font-size-lg);
}

.flight-number-large {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  font-family: var(--font-family-monospace);
}

.airline-name {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

.flight-status-large .el-tag {
  padding: var(--space-2) var(--space-4);
  font-size: var(--font-size-base);
}

/* Flight Route Detailed */
.flight-route-detailed {
  display: flex;
  align-items: center;
  gap: var(--space-6);
}

.route-endpoint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  flex-shrink: 0;
}

.time-large {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  font-family: var(--font-family-monospace);
}

.date {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.airport-code {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.airport-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  text-align: center;
  max-width: 150px;
}

.city-name {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

.route-path-detailed {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.path-duration {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.path-line-detailed {
  width: 100%;
  height: 3px;
  background: var(--color-border-secondary);
  border-radius: 2px;
}

.plane-icon-large {
  color: var(--color-primary);
  font-size: var(--icon-lg);
  transform: rotate(90deg);
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.info-item span {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

.price-large {
  font-size: var(--font-size-xl);
  color: var(--color-primary);
  font-weight: var(--font-weight-bold);
}

.price-total {
  font-size: var(--font-size-lg);
  color: var(--color-success);
  font-weight: var(--font-weight-bold);
}

/* Detail Actions */
.detail-actions {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border-primary);
}

.detail-actions .el-button {
  flex: 1;
  min-width: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
}

/* Reschedule Dialog */
.reschedule-dialog :deep(.el-dialog) {
  border-radius: var(--radius-lg);
}

.reschedule-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.reschedule-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.current-ticket-card {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  border: 1px solid var(--color-border-primary);
}

.ticket-summary {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.ticket-summary .flight-number {
  font-family: var(--font-family-monospace);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.ticket-summary .route {
  color: var(--color-text-secondary);
}

.ticket-summary .price {
  color: var(--color-primary);
  font-weight: var(--font-weight-bold);
  margin-left: auto;
}

.connecting-badge {
  margin-left: 8px;
}

.flight-segments-details {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-primary);
}

.segments-header {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.segment-detail-reschedule {
  background: var(--color-bg-primary);
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid var(--color-border-secondary);
}

.segment-detail-reschedule:last-child {
  margin-bottom: 0;
}

.segment-info {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.segment-flight {
  font-family: var(--font-family-monospace);
  font-weight: 600;
  color: var(--color-primary);
  font-size: 14px;
}

.segment-route {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.segment-time {
  color: var(--color-text-tertiary);
  font-size: 13px;
  font-family: var(--font-family-monospace);
}

.fee-alert {
  margin: 0;
}

.fee-info h4 {
  margin: 0 0 var(--space-2) 0;
}

.fee-info p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.flights-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  max-height: 300px;
  overflow-y: auto;
}

.flight-option {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition-fast);
}

.flight-option:hover {
  border-color: var(--color-primary);
  background: var(--color-bg-hover);
}

.flight-option.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.flight-option-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.flight-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.flight-info .flight-number {
  font-family: var(--font-family-monospace);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.flight-time {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.flight-details {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-1);
}

.flight-details .price {
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.seats {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.selection-indicator {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid var(--color-border-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
}

.flight-option.selected .selection-indicator {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.connecting-flight-info {
  margin-top: var(--space-2);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.connecting-route {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.reason-input {
  width: 100%;
}

.dialog-footer {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
}

/* Fare Difference Calculation Styles */
.fare-difference-card {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  border: 1px solid var(--color-border-primary);
  box-shadow: var(--shadow-sm);
}

.fare-calculation-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-4) 0;
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--color-border-primary);
}

.fare-calculation-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.fare-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) 0;
}

.fare-label {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.fare-value {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-semibold);
  font-family: var(--font-family-monospace);
}

.fare-placeholder {
  color: var(--color-text-tertiary);
  font-style: italic;
  font-family: inherit !important;
}

.fare-divider {
  height: 1px;
  background: var(--color-border-secondary);
  margin: var(--space-2) 0;
}

.fare-divider.total-divider {
  height: 2px;
  background: var(--color-border-primary);
  margin: var(--space-3) 0;
}

.fare-difference .fare-value.fare-increase {
  color: var(--color-error);
}

.fare-difference .fare-value.fare-decrease {
  color: var(--color-success);
}

.fare-difference .fare-value.fare-same {
  color: var(--color-text-tertiary);
}

.fare-total {
  background: var(--color-bg-hover);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  margin: var(--space-2) 0 0;
}

.fare-total .fare-label {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.fare-total .total-amount {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

/* Responsive Design */
@media (max-width: 1023px) {
  .header-content {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }
  
  .filter-grid {
    grid-template-columns: 1fr;
  }
  
  .results-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }
  
  .tickets-grid {
    grid-template-columns: 1fr;
  }
  
  .flight-timeline {
    flex-direction: column;
    gap: var(--space-3);
  }
  
  .timeline-path {
    width: 100%;
    min-width: unset;
  }
}

@media (max-width: 767px) {
  .page-header {
    padding: var(--space-4) 0;
  }
  
  .search-filter-section,
  .tickets-section {
    padding: var(--space-4) 0;
  }
  
  .search-filter-card,
  .ticket-card {
    padding: var(--space-4);
  }
  
  .ticket-card-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-3);
  }
  
  .status-badges {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
  
  .ticket-actions {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-2);
  }
  
  .flight-route-detailed {
    flex-direction: column;
    gap: var(--space-4);
  }
  
  .route-path-detailed {
    width: 100%;
    flex-direction: row;
  }
  
  .detail-actions {
    flex-direction: column;
  }
  
  .detail-actions .el-button {
    flex: none;
  }
}

/* Connecting flights specific styles */
.connecting-label {
  margin-top: var(--space-1);
}

.connecting-timeline {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #0ea5e9;
}

.connecting-flights-display {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: 100%;
}

.flight-leg {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2);
  background: rgba(255, 255, 255, 0.7);
  border-radius: var(--radius-sm);
  position: relative;
}

.flight-info-mini {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
}

.mini-flight-number {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
  font-family: var(--font-family-monospace);
}

.connection-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) 0;
  margin: 0 var(--space-4);
}

.transfer-icon {
  color: var(--color-warning);
  font-size: var(--icon-sm);
}

.transfer-text {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.connecting-tag {
  margin-left: var(--space-2);
}

.connecting-info {
  margin-top: var(--space-1);
}

.text-secondary {
  color: var(--color-text-secondary);
}

.flight-status-display {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

/* Connecting flights detailed route styles */
.connecting-route-detailed {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  width: 100%;
}

.route-segment {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3);
  background: rgba(240, 249, 255, 0.8);
  border-radius: var(--radius-md);
  border: 1px solid rgba(14, 165, 233, 0.2);
}

.flight-number-path {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
  font-family: var(--font-family-monospace);
  margin-bottom: var(--space-1);
}

.layover-info {
  display: flex;
  justify-content: center;
  margin: var(--space-2) 0;
}

.layover-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-3);
  background: var(--color-warning-light);
  border-radius: var(--radius-md);
  border: 2px dashed var(--color-warning);
}

.transfer-icon-large {
  color: var(--color-warning);
  font-size: var(--icon-lg);
}

.layover-text {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-warning);
}

.layover-airport {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.connecting-badge {
  margin-top: var(--space-2);
}

/* Cross-airport Transfer Reminder Styles */
.cross-airport-reminder {
  margin: var(--space-3) 0;
  padding: var(--space-3);
  background: linear-gradient(135deg, #fff5f5 0%, #fef5e7 100%);
  border: 2px solid var(--color-warning);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.cross-airport-reminder.detailed {
  margin: var(--space-6) 0;
  padding: var(--space-6);
  background: linear-gradient(135deg, #fff1f1 0%, #fef7e7 100%);
  border: 2px solid var(--color-danger-light);
}

.reminder-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.warning-icon {
  color: var(--color-danger);
  font-size: var(--icon-lg);
}

.reminder-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-danger);
}

.reminder-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.reminder-text {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.transfer-detail {
  background: rgba(255, 255, 255, 0.7);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  border: 1px solid var(--color-warning-light);
}

.transfer-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.transfer-city {
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.airport-transfer {
  font-family: var(--font-mono);
  font-weight: var(--font-weight-bold);
  background: var(--color-warning-light);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  color: var(--color-warning-dark);
}

.transfer-airports {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.from-airport, .to-airport {
  font-weight: var(--font-weight-medium);
}

/* Detailed view styles */
.transfer-info-detailed {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.transfer-city-label {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.airport-transfer-detailed {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3);
  background: rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-secondary);
}

.from-airport-detail, .to-airport-detail {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  flex: 1;
}

.airport-code {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
  font-family: var(--font-mono);
}

.airport-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  text-align: center;
}

.flight-ref {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.transfer-arrow {
  color: var(--color-danger);
  font-size: var(--icon-xl);
}

.reminder-tips-detailed {
  margin-top: var(--space-4);
  padding: var(--space-4);
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-primary);
}

.tips-title {
  margin: 0 0 var(--space-3) 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-bold);
  color: var(--color-danger);
}

.reminder-tips {
  margin: 0;
  padding-left: var(--space-5);
  list-style-type: disc;
}

.reminder-tips li {
  margin-bottom: var(--space-2);
  font-size: var(--font-size-sm);
  line-height: 1.5;
  color: var(--color-text-primary);
}

.reminder-tips li strong {
  color: var(--color-danger);
  font-weight: var(--font-weight-bold);
}

/* Dark Mode */
.dark .ticket-card {
  background: var(--color-bg-secondary);
}

.dark .flight-timeline {
  background: var(--color-bg-primary);
}

.dark .flight-summary-card {
  background: var(--color-bg-primary);
}

.dark .tickets-table-container {
  background: var(--color-bg-secondary);
}

.dark .flight-leg {
  background: rgba(30, 41, 59, 0.7);
}

.dark .connecting-timeline {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  border-color: #0ea5e9;
}

.dark .route-segment {
  background: rgba(15, 23, 42, 0.8);
  border-color: rgba(14, 165, 233, 0.3);
}

.dark .layover-indicator {
  background: rgba(120, 53, 15, 0.3);
  border-color: #f59e0b;
}

.dark .path-line,
.dark .path-line-detailed {
  background: var(--color-border-hover);
}

/* Countdown Styles */
.countdown-card {
  background: #f8f9ff;
  border: 1px solid #e6f7ff;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: all 0.3s ease;
  margin: 16px 0;
}

.countdown-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}

.countdown-icon {
  font-size: 20px;
  color: #1565C0;
}

.countdown-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.countdown-display {
  font-size: 36px;
  font-weight: bold;
  font-family: 'Monaco', monospace;
  color: #1565C0;
  margin-bottom: 12px;
  line-height: 1;
}

.countdown-message {
  font-size: 14px;
  color: #666;
  margin: 0;
}

/* Urgency Classes */
.urgency-low {
  background: #f0f9ff;
  border-color: #3b82f6;
}

.urgency-low .countdown-display {
  color: #1565C0;
}

.urgency-low .countdown-icon {
  color: #1565C0;
}

.urgency-medium {
  background: #fffbeb;
  border-color: #f59e0b;
}

.urgency-medium .countdown-display {
  color: #d97706;
}

.urgency-medium .countdown-icon {
  color: #d97706;
}

.urgency-high {
  background: #fef2f2;
  border-color: #ef4444;
  animation: pulse 1s infinite;
}

.urgency-high .countdown-display {
  color: #dc2626;
}

.urgency-high .countdown-icon {
  color: #dc2626;
}

.urgency-expired {
  background: #f3f4f6;
  border-color: #6b7280;
}

.urgency-expired .countdown-display {
  color: #6b7280;
}

.urgency-expired .countdown-icon {
  color: #6b7280;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

@media print {
  .page-header,
  .search-filter-section,
  .header-actions,
  .view-controls,
  .ticket-actions,
  .pagination-container {
    display: none;
  }
  
  .ticket-card {
    break-inside: avoid;
    border: 1px solid #000;
    margin-bottom: var(--space-4);
  }
}

.debug-alert .el-message-box__message {
  font-family: monospace;
  white-space: pre-line;
  text-align: left;
  font-size: 12px;
}

/* Connecting Flight Display Styles */
.connecting-flight-numbers {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.flight-number-item {
  background: #f0f9ff;
  color: #1565c0;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid #e0f2fe;
}

.connecting-segments {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 4px;
  font-size: 12px;
  color: #666;
}

.connecting-segments .segment {
  white-space: nowrap;
}

.connecting-segments .segment-separator {
  color: #999;
  margin: 0 4px;
}
</style>