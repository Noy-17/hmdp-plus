// 节流函数
export const throttle = (fn, delay) => {
  let timer = null
  return function (...args) {
    if (timer) return
    timer = setTimeout(() => {
      fn.apply(this, args)
      timer = null
    }, delay)
  }
}

// 获取滚动位置
export const getScrollTop = () => {
  return document.documentElement.scrollTop || document.body.scrollTop
}

// 设置滚动位置
export const setScrollTop = (value) => {
  document.documentElement.scrollTop = value
  document.body.scrollTop = value
}
