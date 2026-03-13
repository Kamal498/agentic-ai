import logging
import requests
from typing import List, Dict, Any
from config import SPRING_API_BASE

logger = logging.getLogger(__name__)


def fetch_orders() -> List[Dict[str, Any]]:
    resp = requests.get(f"{SPRING_API_BASE}/api/orders", timeout=10)
    resp.raise_for_status()
    orders = resp.json()
    logger.info(f"Fetched {len(orders)} orders from API")
    return orders


def fetch_inventory() -> List[Dict[str, Any]]:
    resp = requests.get(f"{SPRING_API_BASE}/api/inventory", timeout=10)
    resp.raise_for_status()
    items = resp.json()
    logger.info(f"Fetched {len(items)} inventory items from API")
    return items


def fetch_payments() -> List[Dict[str, Any]]:
    resp = requests.get(f"{SPRING_API_BASE}/api/payments", timeout=10)
    resp.raise_for_status()
    payments = resp.json()
    logger.info(f"Fetched {len(payments)} payments from API")
    return payments


def order_to_text(order: Dict[str, Any]) -> str:
    items_text = ", ".join(
        f"{item.get('quantity')}x {item.get('productName')} @ ${item.get('unitPrice')}"
        for item in order.get("items", [])
    )
    return (
        f"Order Number: {order.get('orderNumber')} | "
        f"Customer: {order.get('customerName')} ({order.get('customerId')}) | "
        f"Email: {order.get('customerEmail')} | "
        f"Status: {order.get('status')} | "
        f"Total: ${order.get('totalAmount')} | "
        f"Description: {order.get('description', 'N/A')} | "
        f"Shipping: {order.get('shippingAddress', 'N/A')} | "
        f"Items: [{items_text}] | "
        f"Date: {order.get('orderDate')}"
    )


def inventory_to_text(item: Dict[str, Any]) -> str:
    stock_status = "LOW STOCK" if item.get("lowStock") else "In Stock"
    return (
        f"Product ID: {item.get('productId')} | "
        f"Name: {item.get('productName')} | "
        f"Category: {item.get('category')} | "
        f"Price: ${item.get('unitPrice')} | "
        f"Available: {item.get('quantityAvailable')} units | "
        f"Stock Status: {stock_status} | "
        f"Reorder Threshold: {item.get('reorderThreshold')} | "
        f"Supplier: {item.get('supplier', 'N/A')} | "
        f"Description: {item.get('description', 'N/A')}"
    )


def payment_to_text(payment: Dict[str, Any]) -> str:
    return (
        f"Payment ID: {payment.get('paymentId')} | "
        f"Order ID: {payment.get('orderId')} | "
        f"Customer: {payment.get('customerId')} | "
        f"Amount: ${payment.get('amount')} | "
        f"Method: {payment.get('paymentMethod')} | "
        f"Status: {payment.get('status')} | "
        f"Transaction: {payment.get('transactionId', 'N/A')} | "
        f"Date: {payment.get('paymentDate')} | "
        f"Notes: {payment.get('notes', 'N/A')}"
    )
