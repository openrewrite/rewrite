#!/bin/bash
echo "=== Differences between JavaSender and JavaReceiver ==="
echo ""

# Extract just the element lines (skip headers)
grep "^- " java-rpc-sender-summary.txt | sort > /tmp/sender-sorted.txt
grep "^- " java-rpc-receiver-summary.txt | sort > /tmp/receiver-sorted.txt

# Find differences
echo "Elements only in Sender:"
comm -23 /tmp/sender-sorted.txt /tmp/receiver-sorted.txt | head -20
echo ""
echo "Elements only in Receiver:"
comm -13 /tmp/sender-sorted.txt /tmp/receiver-sorted.txt | head -20
echo ""
echo "Elements with different properties:"
diff <(grep "^- " java-rpc-sender-summary.txt | sort) <(grep "^- " java-rpc-receiver-summary.txt | sort) | grep "^[<>]" | head -40
