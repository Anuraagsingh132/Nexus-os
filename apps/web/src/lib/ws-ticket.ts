export async function getWsTicket(): Promise<string> {
  try {
    const response = await fetch('/api/v1/ws/ticket', {
      method: 'POST',
      credentials: 'include',
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch WS ticket: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    
    // Assuming the API returns { ticket: '...' }
    if (!data.ticket) {
      throw new Error('Ticket not found in response');
    }

    return data.ticket;
  } catch (error) {
    console.error('Error fetching WS ticket:', error);
    throw error;
  }
}
