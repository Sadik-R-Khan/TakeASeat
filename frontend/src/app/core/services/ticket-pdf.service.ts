import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import { Booking } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class TicketPdfService {

  /**
   * Builds a clean, printable PDF ticket on the client using jsPDF.
   */
  generateTicketDocument(booking: Booking): jsPDF {
    const doc = new jsPDF({ unit: 'pt', format: 'a5', orientation: 'portrait' });
    const pageWidth = doc.internal.pageSize.getWidth();

    // Header band
    doc.setFillColor(26, 29, 33);
    doc.rect(0, 0, pageWidth, 68, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(20);
    doc.text('TakeASeat', 28, 36);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    doc.text('ADMIT ONE — E-TICKET', 28, 54);

    // Reference & Status
    doc.setTextColor(26, 29, 33);
    doc.setFont('courier', 'bold');
    doc.setFontSize(11);
    doc.text(`REF: ${booking.bookingReference}`, 28, 96);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    if (booking.status === 'CONFIRMED') {
      doc.setTextColor(21, 128, 61);
    } else {
      doc.setTextColor(185, 28, 28);
    }
    doc.text(booking.status, pageWidth - 28, 96, { align: 'right' });

    // Divider
    doc.setDrawColor(216, 222, 230);
    doc.line(28, 108, pageWidth - 28, 108);

    // Movie title
    doc.setTextColor(26, 29, 33);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(17);
    doc.text(booking.movieTitle, 28, 136);

    // Details grid
    let y = 166;
    const addRow = (label: string, value: string, monospace = false) => {
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(9);
      doc.setTextColor(92, 99, 112);
      doc.text(label.toUpperCase(), 28, y);

      doc.setFont(monospace ? 'courier' : 'helvetica', 'normal');
      doc.setFontSize(11);
      doc.setTextColor(26, 29, 33);
      doc.text(value, 28, y + 16);
      y += 42;
    };

    const showDate = new Date(booking.showTime);
    const formattedTime = isNaN(showDate.getTime())
      ? booking.showTime
      : showDate.toLocaleString(undefined, {
          weekday: 'short',
          year: 'numeric',
          month: 'short',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        });

    addRow('Theatre', `${booking.theatreName} — ${booking.theatreCity}`);
    addRow('Address', booking.theatreAddress);
    addRow('Showtime', formattedTime);
    addRow('Seats (5x10 Layout)', booking.seats.join(', '), true);
    addRow('Ticket Holder', `${booking.userName} (${booking.userEmail})`);
    addRow('Total Paid', `INR ${Number(booking.totalAmount).toFixed(2)}`);

    // Footer
    doc.setDrawColor(216, 222, 230);
    doc.line(28, y + 4, pageWidth - 28, y + 4);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(92, 99, 112);
    doc.text('Present this ticket reference at the theatre entrance.', 28, y + 24);
    doc.text(`Issued: ${new Date(booking.createdAt).toLocaleString()}`, 28, y + 38);

    return doc;
  }

  createTicketBlob(booking: Booking): Blob {
    const doc = this.generateTicketDocument(booking);
    return doc.output('blob');
  }

  downloadTicket(booking: Booking): void {
    const doc = this.generateTicketDocument(booking);
    doc.save(`${booking.bookingReference}.pdf`);
  }
}
