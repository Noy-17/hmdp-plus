#!/bin/bash
# ============================================================
# Nacos 配置上传脚本
# 将所有 docs/nacos-config/*.yaml 发布到 Nacos 配置中心
#
# 用法:
#   bash upload.sh                           # 上传到默认 localhost:8848
#   bash upload.sh 192.168.137.128:8848      # 上传到指定 Nacos
#   bash upload.sh 192.168.137.128:8848 dev  # 指定 namespace (非 public)
# ============================================================

NACOS_ADDR="${1:-localhost:8848}"
NACOS_NAMESPACE="${2:-}"
NACOS_GROUP="DEFAULT_GROUP"
NACOS_USER="${NACOS_USERNAME:-nacos}"
NACOS_PASS="${NACOS_PASSWORD:-nacos}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Nacos 配置上传 ==="
echo "地址:   ${NACOS_ADDR}"
echo "命名空间: ${NACOS_NAMESPACE:-public}"
echo ""

# 待上传的配置文件列表
# 格式: "dataId|file"
FILES=(
    "hmdp-common-config.yaml|${SCRIPT_DIR}/hmdp-common-config.yaml"
    "hmdp-shop-service.yaml|${SCRIPT_DIR}/hmdp-shop-service.yaml"
    "hmdp-user-service.yaml|${SCRIPT_DIR}/hmdp-user-service.yaml"
    "hmdp-voucher-service.yaml|${SCRIPT_DIR}/hmdp-voucher-service.yaml"
    "hmdp-blog-service.yaml|${SCRIPT_DIR}/hmdp-blog-service.yaml"
    "hmdp-follow-service.yaml|${SCRIPT_DIR}/hmdp-follow-service.yaml"
    "hmdp-gateway.yaml|${SCRIPT_DIR}/hmdp-gateway.yaml"
)

fail=0

for entry in "${FILES[@]}"; do
    dataId="${entry%%|*}"
    file="${entry##*|}"

    if [ ! -f "$file" ]; then
        echo "[SKIP] 文件不存在: $file"
        continue
    fi

    content=$(cat "$file")

    echo -n "[UPLOAD] $dataId ... "

    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
        --data-urlencode "dataId=${dataId}" \
        --data-urlencode "group=${NACOS_GROUP}" \
        --data-urlencode "content=${content}" \
        --data-urlencode "tenant=${NACOS_NAMESPACE}" \
        ${NACOS_USER:+-u "${NACOS_USER}:${NACOS_PASS}"})

    if [ "$http_code" = "200" ]; then
        echo "OK"
    else
        echo "FAIL (HTTP ${http_code})"
        fail=1
    fi
done

echo ""
if [ $fail -eq 0 ]; then
    echo "全部上传成功"
else
    echo "部分上传失败，请检查 Nacos 是否可达"
fi
