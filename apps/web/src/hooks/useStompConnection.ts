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
  const attemptRef = useRef(0);
  const INITIAL_RECONNECT_DELAY = 2000;
  const MAX_RECONNECT_DELAY = 30000;
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

    // Exponential backoff with jitter: baseDelay * (1.5 ^ attempt) + Math.random() * 1000
    attemptRef.current += 1;
    const computedDelay = INITIAL_RECONNECT_DELAY * Math.pow(1.5, attemptRef.current) + Math.random() * 1000;
    const finalDelay = Math.min(computedDelay, MAX_RECONNECT_DELAY);

    reconnectTimeoutRef.current = setTimeout(() => {
      connect(true);
    }, finalDelay);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const connect = useCallback(async (isReconnect = false) => {
    if (!isMounted.current) return;

    setStatus(isReconnect ? 'reconnecting' : 'connecting');

    try {
      const ticket = await getWsTicket();
      
      if (!isMounted.current) return;

      const wsUrl = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws';
      const newClient = new Client({
        webSocketFactory: () => new SockJS(`${wsUrl}?ticket=${ticket}`),
        connectHeaders: {
          ticket: ticket
        },
        reconnectDelay: 0, // Handled via explicit ticket-refresh backoff loop
        onConnect: () => {
          if (!isMounted.current) {
            newClient.deactivate();
            return;
          }
          setStatus('connected');
          setClient(newClient);
          attemptRef.current = 0; // Reset backoff attempt counter on clean connection
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
