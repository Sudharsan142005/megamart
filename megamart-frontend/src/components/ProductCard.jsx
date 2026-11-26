import { Link } from "react-router-dom";
export default function ProductCard({ product, onAdd }) {
  return (
    <article className="product-card">
      <Link to={`/products/${product.id}`} className="product-image">
        <img
          src={
            product.imageUrl ||
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=700"
          }
          alt={product.name}
        />
        {product.stockQuantity < 6 && <span className="badge">Low stock</span>}
      </Link>
      <div className="product-meta">
        <span>{product.categoryName || "Featured"}</span>
        <span>{product.stockQuantity > 0 ? "In stock" : "Sold out"}</span>
      </div>
      <Link className="product-name" to={`/products/${product.id}`}>
        {product.name}
      </Link>
      <div className="product-bottom">
        <strong>${Number(product.price).toFixed(2)}</strong>
        {onAdd && (
          <button
            disabled={!product.stockQuantity}
            className="add-button"
            onClick={() => onAdd(product)}
          >
            Add +
          </button>
        )}
      </div>
    </article>
  );
}
