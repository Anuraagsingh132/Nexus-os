import { useState, useEffect, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getWsTicket } from '../lib/ws-ticket';

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'reconnecting';

export function useStompConnection() {
  const [status, setStatus] = useState<ConnectionStatus>('disconnected');
  const [client, setClient] = useState<Client | null>(null);
  
  const clientRef = useRef<Client | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectDelayRef = useRef(2000);
  const maxReconnectDelay = 30000;
  const isMounted = useRef(true);

  const clearReconnectTimeout = () => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
  };

  const scheduleReconnect = useCallback(() => {
    if (!isMounted.current) return;
    
    setStatus('disconnected');
    setClient(null);
    
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }

    clearReconnectTimeout();

    reconnectTimeoutRef.current = setTimeout(() => {
      connect(true);
      reconnectDelayRef.current = Math.min(reconnectDelayRef.current * 1.5, maxReconnectDelay);
    }, reconnectDelayRef.current);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const connect = useCallback(async (isReconnect = false) => {
    if (!isMounted.current) return;

    setStatus(isReconnect ? 'reconnecting' : 'connecting');

    try {
      const ticket = await getWsTicket();
      
      if (!isMounted.current) return;

      const newClient = new Client({
        webSocketFactory: () => new SockJS(`/ws?ticket=${ticket}`),
        connectHeaders: {
          ticket: ticket
        },
        reconnectDelay: 0, // Disable built-in reconnect to fetch a new ticket every time
        onConnect: () => {
          if (!isMounted.current) {
            newClient.deactivate();
            return;
          }
          setStatus('connected');
          setClient(newClient);
          reconnectDelayRef.current = 2000; // Reset backoff
        },
        onWebSocketClose: () => {
          scheduleReconnect();
        },
        onWebSocketError: () => {
          scheduleReconnect();
        },
        onStompError: (frame) => {
          console.error('STOMP Error:', frame);
          scheduleReconnect();
        }
      });

      clientRef.current = newClient;
      newClient.activate();
    } catch (error) {
      console.error('Failed to get WS ticket or connect STOMP:', error);
      scheduleReconnect();
    }
  }, [scheduleReconnect]);

  useEffect(() => {
    isMounted.current = true;
    connect();

    return () => {
      isMounted.current = false;
      clearReconnectTimeout();
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
      }
    };
  }, [connect]);

  return { status, client };
}
