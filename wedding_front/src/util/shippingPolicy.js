export const FREE_SHIPPING_THRESHOLD = 30000;
export const SHIPPING_FEE = 3000;

export const calculateShippingFee = (productSubtotal) => {
  return productSubtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
};
