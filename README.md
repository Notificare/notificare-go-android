# Actito GO

## Supported deep links

The GO app supports several deep links. You can invoke those manually, or send them in a notification! 🚀

| Action                   | Deep link                                     |
|--------------------------|-----------------------------------------------|
| Open the home tab        | `re.notifica.go://notifica.re/home`           |
| Open the cart tab        | `re.notifica.go://notifica.re/cart`           |
| Open the settings tab    | `re.notifica.go://notifica.re/settings`       |
| Open the inbox           | `re.notifica.go://notifica.re/inbox`          |
| Open the user profile    | `re.notifica.go://notifica.re/profile`        |
| Open the products list   | `re.notifica.go://notifica.re/products`       |
| Open the product details | `re.notifica.go://notifica.re/product?id=foo` |
| Open the events builder  | `re.notifica.go://notifica.re/events`         |

## Supported custom events

Use the built-in custom events to create and experiment with automations! 🤖

| Event                         | Description                                                       |
|-------------------------------|-------------------------------------------------------------------|
| `intro_finished`              | Submitted when the user finishes the introduction.                |
| `page_viewed.home`            | Submitted when the home tab is displayed.                         |
| `page_viewed.cart`            | Submitted when the cart tab is displayed.                         |
| `page_viewed.settings`        | Submitted when the settings tab is displayed.                     |
| `page_viewed.inbox`           | Submitted when the inbox is displayed.                            |
| `page_viewed.user_profile`    | Submitted when the user profile is displayed.                     |
| `page_viewed.events`          | Submitted when the events builder is displayed.                   |
| `page_viewed.products`        | Submitted when the products list is displayed.                    |
| `page_viewed.product_details` | Submitted when the product details is displayed.                  |
| `add_to_cart`                 | Submitted when the user adds a product to the cart. &#x00B9;      |
| `remove_from_cart`            | Submitted when the user removes a product from the cart. &#x00B9; |
| `cart_updated`                | Submitted when the cart is updated. &#x00B2;                      |
| `cart_cleared`                | Submitted when the cart is cleared.                               |
| `purchase`                    | Submitted when the user completes a purchase. &#x00B2;            |
| `product_viewed`              | Submitted when the user view the details of a product. &#x00B9;   |

&#x00B9; Data object includes a `ProductRepresentation`.

&#x00B2; Data object includes a `ProductsOverviewRepresentation`.

### Event data schemas

##### `ProductRepresentation`

```
{
  id: String
  name: String
  price: Double
  price_formatted: String
}
```

##### `ProductsOverviewRepresentation`

```
{
  total_price: Double
  total_price_formatted: String
  total_items: Int
  products: Array<ProductRepresentation>
```

## Supported Live Activities

### Coffee Brewer

#### Expected `content-state`

```json
{
  "state": "brewing",
  "remaining": 4
}
```

#### Possible `state` values

- `grinding`: the initial state, while grinding the coffee beans.
- `brewing`: the intermediate state, while brewing the coffee.
- `served`: the final state, when the coffee has been served.

### Order Status

When you place a fictitious order on the Cart view, a Live Activity is automatically created.
You can modify the state from the Dashboard or the API.

#### Expected `content-state`

```json
{
  "state": "shipped"
}
```

#### Possible `state` values

- `preparing`: the initial state, while preparing the order.
- `shipped`: the intermediate state, while shipping the order.
- `delivered`: the final state, when the order has been delivered.
